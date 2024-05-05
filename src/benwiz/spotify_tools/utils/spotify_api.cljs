(ns benwiz.spotify-tools.utils.spotify-api
  (:require [benwiz.spotify-tools.config :as config]
            [clojure.string :as str]
            [goog.object]
            [re-frame.core :as rf]))

(defn valid-token!
  "Return token if it is still valid. If it is invalid,
  return nil and remove the localstorage entry."
  [token]
  (when (some? token)
    (if (< (js/Date.) (:expires-at token))
      token
      (do ;; since it is expired, delete the localstorage
        (.removeItem (.-localStorage js/window) "spotify-token")
        nil))))

(defn redirect!
  [client-id]
  (let [scopes            (js/encodeURIComponent
                            (str/join " " ["playlist-read-private"
                                           "playlist-modify-private"
                                           "playlist-modify-public"
                                           "user-library-read"
                                           "user-read-private"
                                           "user-read-playback-state"
                                           "user-modify-playback-state"]))
        redirect-uri      (.. js/window -location -origin)
        path              (.. js/window -location -pathname)
        hash              (.. js/window -location -hash)
        spotify-login-uri (str "https://accounts.spotify.com/authorize"
                               "?response_type=" "token" ;; TODO optimization: use `code` but need backend to continue the authorization, allows for refreshing
                               "&client_id=" client-id
                               "&scope=" scopes
                               "&redirect_uri=" (str redirect-uri path)
                               "&state=" (js/encodeURIComponent hash))]
    ;; Redirect, so doesn't matter what is returned
    (set! js/window.location spotify-login-uri)))

(defn- track-artists-set [tracks]
  (into #{}
        (comp
          (map :track)
          (mapcat :artists)
          (map :id))
        (:items tracks)))

(defn get-users-playlists [key token]
  [:benwiz.spotify-tools.events/http-request
   {:key       key
    :method    :get
    :uri       "https://api.spotify.com/v1/me/playlists?limit=50&offset=0"
    :headers   {:Authorization
                (str "Bearer " (:access-token token))}
    :update-db (fn [db result]
                 (-> db
                     (update-in [:spotify/user-playlists :items]
                                (fn [playlists]
                                  (into (or playlists {})
                                        (comp
                                          (map #(update % :track select-keys [:id :name :images :tracks]))
                                          (map (fn [playlist]
                                                 [(:id playlist) playlist])))
                                        (:items result))))
                     (assoc-in [:spotify/user-playlists :total] (:total result))))
    :next      (fn [options result]
                 (when-some [uri (:next result)]
                   (assoc options :uri uri)))
    :update-fx (fn [fx result]
                 ;; TODO when entirely done I want to dispatch event to write localstorage
                 (-> (or fx [])
                     (into
                       (comp
                         (filter (fn [playlist] ;; for now, only pull tracks for genre playlist. Helps avoid 429 but also helps pulling some giant >5000 track playlist
                                   (str/starts-with? (:name playlist) "genre :: ")))
                         (map (fn [playlist]
                                [:dispatch
                                 [:benwiz.spotify-tools.events/http-request
                                  {:method    :get
                                   :uri       (:href (:tracks playlist))
                                   :headers   {:Authorization (str "Bearer " (:access-token token))}
                                   :update-db (fn [db result]
                                                (update-in db [:spotify/user-playlists :items (:id playlist) :tracks :items]
                                                           #(into (or % #{}) (:items result))))
                                   :next      (fn [options result]
                                                (when-some [uri (:next result)]
                                                  (assoc options :uri uri)))}]])))
                       (:items result))
                     #_(into
                         (when-not (:next result)
                           [[:dispatch [:benwiz.spotify-tools.events/set-local-store "spotify/user-playlists"
                                        (fn [db] (:spotify/user-playlists db))]]]))))}])

(defn get-users-tracks [key token limit]
  [:benwiz.spotify-tools.events/http-request
   {:key       key
    :method    :get
    :uri       "https://api.spotify.com/v1/me/tracks?limit=50&offset=0"
    :headers   {:Authorization (str "Bearer " (:access-token token))}
    :update-db (fn [db result]
                 (-> db
                     (update-in [:spotify/user-tracks :items]
                                (fn [tracks]
                                  (into (or tracks #{})
                                        (map #(update % :track select-keys [:id :name :album :artists]))
                                        (:items result))))
                     (assoc-in [:spotify/user-tracks :total] (:total result))))
    :next      (if limit
                 (fn [options result]
                   (when-some [uri (:next result)]
                     (when (some-> (.. (js/URL. uri) -searchParams (get "offset"))
                                   not-empty
                                   js/parseInt
                                   (< limit))
                       (assoc options :uri uri))))
                 (fn [options result]
                   (when-some [uri (:next result)]
                     (assoc options :uri uri))))
    :update-fx (fn [fx result db]
                 (into (or fx [])
                       #_(when (or (nil? (:next result))
                                 (and limit
                                      (some-> (:next result)
                                              js/URL.
                                              (.. -searchParams (get "offset"))
                                              not-empty
                                              js/parseInt
                                              (>= limit))))
                         (let [size 500]
                           (into [[:dispatch
                                   [:benwiz.spotify-tools.events/set-local-store
                                    "spotify/user-tracks" {:total   (:total (:spotify/user-tracks db))
                                                           :indices (js/Math.ceil (/ (count (:items (:spotify/user-tracks db))) size))}]]]
                                 (comp
                                   (partition-all size)
                                   (map-indexed (fn [idx tracks]
                                                  (prn 'count (count (str tracks)))
                                                  [:dispatch
                                                   [:benwiz.spotify-tools.events/set-local-store
                                                    (str "spotify/user-tracks." idx) (str tracks)]])))
                                 (:items (:spotify/user-tracks db)))))))}])

(defn get-artists [token ids]
  (let [uris (into []
                   (comp
                     (partition-all 50)
                     (map #(str/join "," %))
                     (map #(str "https://api.spotify.com/v1/artists?ids=" %)))
                   ids)]
    ;; NOTE I am running these queries sequentially, I could consider doing them in parallel
    ;; just wanted to minimize change of 429 for now.
    [:benwiz.spotify-tools.events/http-request
     {:key       :artists-request
      :method    :get
      :uri       (first uris)
      :uris      (rest uris)
      :headers   {:Authorization (str "Bearer " (:access-token token))}
      :update-db (fn [db result]
                   (update db :spotify/user-artists
                           (fn [artists]
                             (into (or artists {})
                                   (map (fn [artist]
                                          [(:id artist) artist]))
                                   (:artists result)))))
      :next      (fn [options _result]
                   (when-some [next-uri (first (:uris options))]
                     (-> options
                         (assoc :uri next-uri)
                         (update :uris rest))))
      :update-fx (fn [fx result & {:keys [db]}]
                   (into (or fx [])
                         (when-not (:next result)
                           [#_[:dispatch [:benwiz.spotify-tools.events/set-local-store "spotify/user-artists"
                                        (fn [db] (:spotify/user-artists db))]]])))}]))

(defn get-devices [token]
  [:benwiz.spotify-tools.events/http-request
   {:key       :get-devices
    :method    :get
    :uri       "https://api.spotify.com/v1/me/player/devices"
    :headers   {:Authorization (str "Bearer " (:access-token token))}
    :update-db (fn [db result]
                 (assoc db :spotify/devices (:devices result)))}])

(defn get-audio-features [key token ids]
  [:benwiz.spotify-tools.events/http-request
   {:key       key
    :method    :get
    :uri       (str "https://api.spotify.com/v1/audio-features?ids="
                    (str/join "," ids))
    :headers   {:Authorization (str "Bearer " (:access-token token))}
    :update-db (fn [db result]
                 (-> db
                     (update :spotify/audio-features
                                (fn [{:keys [items total]}]
                                  (let [updated-items
                                        (into (or items {})
                                              (map (juxt :id identity))
                                              (:audio_features result))]
                                    {:items updated-items})))))
    #_#_:next      (if limit
                 (fn [options result]
                   (when-some [uri (:next result)]
                     (when (some-> (.. (js/URL. uri) -searchParams (get "offset"))
                                   not-empty
                                   js/parseInt
                                   (< limit))
                       (assoc options :uri uri))))
                 (fn [options result]
                   (when-some [uri (:next result)]
                     (assoc options :uri uri))))}])

(defn get-playback-state [token]
  [:benwiz.spotify-tools.events/http-request
   {:key       :get-playback-state
    :method    :get
    :uri       "https://api.spotify.com/v1/me/player"
    :headers   {:Authorization (str "Bearer " (:access-token token))}
    :update-db (fn [db result]
                 (assoc db :spotify/playback-state result))}])

(defn transfer-playback! [token device-id]
  [:benwiz.spotify-tools.events/http-request
   {:key       :transfer-playback
    :method    :put
    :uri       (str "https://api.spotify.com/v1/me/player")
    :headers   {:Authorization (str "Bearer " (:access-token token))}
    :params    {:device_ids [device-id]}
    :update-fx (fn [fx result]
                 (into fx
                       ;; having both direct db edit and api call may be overkill,
                       ;; if it turns out to be so, keep the api query
                       [[:dispatch [:benwiz.spotify-tools.events/edit-db
                                    (fn [db device-id]
                                      (update db :spotify/devices
                                              #(into []
                                                     (map (fn [device]
                                                            (assoc device :is_active
                                                                   (= (:id device) device-id))))
                                                     %)))
                                    device-id]]
                        [:dispatch (get-devices token)]]))}])

(defn skip-to-next! [token]
  [:benwiz.spotify-tools.events/http-request
   {:key     :skip-to-next
    :method  :post
    :uri     "https://api.spotify.com/v1/me/player/next"
    :headers {:Authorization (str "Bearer " (:access-token token))}}])

(defn seek-to-position! [token position-ms]
  [:benwiz.spotify-tools.events/http-request
   {:key        :seek-to-position
    :method     :put
    :uri        "https://api.spotify.com/v1/me/player/seek"
    :headers    {:Authorization (str "Bearer " (:access-token token))}
    :url-params {:position_ms position-ms}}])

(defn delete-playlist! [token id]
  [:benwiz.spotify-tools.events/http-request
   {:key       (keyword :delete-playlist id)
    :method    :delete
    :uri       (str "https://api.spotify.com/v1/playlists/"
                    id
                    "/followers")
    :headers   {:Authorization
                (str "Bearer " (:access-token token))}
    :update-db (fn [db _result]
                 (-> db
                     (update-in [:spotify/user-playlists :items] dissoc id)
                     (update-in [:spotify/user-playlists :total] dec)))}])

(defn download-user-tracks [token debug?]
  (rf/dispatch
    (let [get-users-tracks
          (get-users-tracks :tracks-request token
                            (if debug?
                              config/debug-track-limit
                              false))
          options (second get-users-tracks)]
      (update-in get-users-tracks [1 :update-fx]
                 (fn [update-fx]
                   (let [f (fn [fx result db]
                             ;; when completely done
                             (when (nil? ((:next options) options result))
                               ;; TODO always downloading this extra data is not what we want, they aren't used together. It'd be better to signal "done" somehow and have these be requestable if the data is ready.
                               (into (or fx [])
                                     [[:dispatch
                                       [:benwiz.spotify-tools.events/db-aware-event
                                        (fn [db]
                                          (when (and (empty? (:spotify/user-artists db))
                                                     (not-empty (:spotify/user-tracks db))
                                                     (not= (:tracks-request db) :loading))
                                            {:fx [[:dispatch (get-artists token (track-artists-set (:spotify/user-tracks db)))]]}))]]
                                      [:dispatch
                                       [:benwiz.spotify-tools.events/db-aware-event
                                        (fn [db]
                                          (when (and (empty? (:spotify/track-features db))
                                                     (not-empty (:spotify/user-tracks db))
                                                     (not= (:features-request db) :loading))
                                            {:fx (into []
                                                       (comp
                                                         (partition-all 100)
                                                         (map (fn [tracks]
                                                                [:dispatch (get-audio-features :audio-features-request token
                                                                                               (into [] (map (comp :id :track)) tracks))])))
                                                       (:items (:spotify/user-tracks db)))}))]]])))]
                     (if update-fx
                       (comp f update-fx)
                       f)))))))

(defn download-user-data [token debug?]
  (rf/dispatch (get-users-playlists :playlists-request token))
  (download-user-tracks token debug?))

(defn search [token term]
  [:benwiz.spotify-tools.events/http-request
   {:key        :search
    :method     :get
    :uri        "https://api.spotify.com/v1/search"
    :headers    {:Authorization (str "Bearer " (:access-token token))}
    :url-params {:q      term
                 :type   ["track" #_"artist"]
                 :limit  20
                 :offset 0}
    :update-db  (fn [db result]
                  (assoc db :spotify/search-tracks (:items (:tracks result))))}])

(defn audio-features [token id]
  [:benwiz.spotify-tools.events/http-request
   {:key        (keyword :audio-features id)
    :method     :get
    :uri        (str "https://api.spotify.com/v1/audio-features/" id)
    :headers    {:Authorization (str "Bearer " (:access-token token))}
    :update-db  (fn [db result]
                  (assoc db :analysis/audio-features result))}])
