(ns benwiz.spotify-tools.views.genre-playlists
  (:require
   ["@mui/icons-material/Clear" :default ClearIcon]
   ["@mui/icons-material/Delete" :default DeleteIcon]
   ["@mui/icons-material/FavoriteBorder" :default FavoriteBorderIcon]
   ["@mui/icons-material/Refresh" :default RefreshIcon]
   ["@mui/icons-material/Search" :default SearchIcon]
   ["@mui/icons-material/Sync" :default SyncIcon]
   ["@mui/joy/Button" :default Button]
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/CircularProgress" :default CircularProgress]
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/IconButton" :default IconButton]
   ["@mui/joy/Input" :default Input]
   ["@mui/joy/LinearProgress" :default LinearProgress]
   ["@mui/joy/List" :default List]
   ["@mui/joy/ListDivider" :default ListDivider]
   ["@mui/joy/ListItem" :default ListItem]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   ["@mui/material/Icon" :default Icon]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.links :as links]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; Util fns, atoms

(defn count-items [v] (count (:items v)))

(defn app-loading? [db]
  (or (= (:playlists-request db) :loading)
      (= (:tracks-request db) :loading)
      (= (:artists-request db) :loading)))

(defn genre-playlists [v]
  (into []
        (comp
          (map second)
          (filter (fn [playlist]
                    (str/starts-with? (:name playlist) "genre :: "))))
        (:items v)))

(defn build-genre-playlists [db]
  (let [tracks            (-> db :spotify/user-tracks :items)
        artists           (:spotify/user-artists db)
        playlists         (-> db :spotify/user-playlists :items)
        genre-count       (when (not-empty tracks)
                            (frequencies
                              (into []
                                    (comp
                                      (map :track)
                                      (map :artists)
                                      (mapcat (fn [as] ;; if multiple artists, dedupe genres across them
                                                (into #{}
                                                      (comp
                                                        (keep #(get artists (:id %)))
                                                        (mapcat :genres))
                                                      as))))
                                    tracks)))
        playlists-by-name (into {}
                                (comp
                                  (map second)
                                  (map (fn [{:keys [name] :as playlist}]
                                         [name playlist])))
                                playlists)]
    (->> (into []
               (comp
                 (map second)
                 (mapcat :genres)
                 (distinct)
                 (map (fn [genre]
                        (let [playlist-name (str "genre :: " genre)
                              playlist      (get playlists-by-name playlist-name)
                              c             (-> playlist :tracks :total)
                              target-c      (get genre-count genre)]
                          {:name         playlist-name
                           :genre        genre
                           :label        (str genre (when (or c target-c)
                                                      (str " ("
                                                           (when (and c (< c target-c))
                                                             (str c " -> "))
                                                           (max c target-c) ")")))
                           :id           (:id playlist (str "tempid-" genre))
                           :exists?      (some? (:id playlist))
                           :count        c
                           :target-count target-c}))))
               artists)
         (sort-by (juxt (comp not :exists?)
                        (comp - :target-count)
                        :label))
         (into [])
         not-empty)))

(defn genre-playlist-options [playlists]
  (into []
        (comp
          (map second)
          (filter (fn [playlist]
                    (str/starts-with? (str (:name playlist)) "genre :: ")))
          (map (fn [playlist]
                 {:id    (:id playlist)
                  :label (str (str/replace (str (:name playlist)) "genre :: " "")
                              " (" (-> playlist :tracks :total) ")")})))
        (:items playlists)))

(defn build-genre->track-ids [db]
  (let [artists          (:spotify/user-artists db)
        tracks           (-> db :spotify/user-tracks :items)
        genres           (into #{}
                               (comp
                                 (map :track)
                                 (mapcat :artists)
                                 (keep #(get artists (:id %)))
                                 (mapcat :genres))
                               tracks)
        track-id->genres (into {}
                               (comp
                                 (map :track)
                                 (map (fn [track]
                                        [(:id track)
                                         (into #{}
                                               (comp
                                                 (map :id)
                                                 (keep #(get artists %))
                                                 (mapcat :genres))
                                               (:artists track))])))
                               tracks)]
    (into {}
          (map (fn [genre]
                 [genre (into #{}
                              (comp
                                (filter (fn [[_track genres]]
                                          (genres genre)))
                                (map first))
                              track-id->genres)]))
          genres)))

(defn genre-track-ids [genre]
  (fn [db]
    (let [artists (:spotify/user-artists db)]
      (into #{}
            (comp
              (map :track)
              (map (fn [track]
                     (assoc track :genres
                            (into #{}
                                  (comp
                                    (map :id)
                                    (keep #(get artists %))
                                    (mapcat :genres))
                                  (:artists track)))))
              (filter (fn [track]
                        (contains? (:genres track) genre)))
              (map :id))
            (-> db :spotify/user-tracks :items)))))

(def genre-playlists-model (r/atom #{}))

;; Components

(defn reload-spotify-data-button []
  (let [token  (rf/subscribe [::subs/db :spotify/token])
        debug? (rf/subscribe [::subs/db :debug?])]
    [:> Button {:variant        "plain"
                :color          "success"
                :startDecorator (r/as-element [:> RefreshIcon])
                :onClick        (fn [_]
                                  (rf/dispatch [::events/edit-db assoc :playlists-request :loading])
                                  (rf/dispatch [::events/edit-db assoc :tracks-request :loading])
                                  (rf/dispatch [::events/edit-db assoc :artists-request :loading])
                                  (rf/dispatch [::events/edit-db assoc :playlists-request :loading])
                                  (rf/dispatch [::events/edit-db assoc :tracks-request :loading])
                                  (rf/dispatch [::events/edit-db assoc :artists-request :loading])
                                  (rf/dispatch [::events/remove-local-store "spotify/user-playlists"])
                                  (rf/dispatch [::events/remove-local-store "spotify/user-tracks"])
                                  ;; TODO need to read then remove all the indices
                                  (rf/dispatch [::events/remove-local-store "spotify/user-artists"])
                                  (rf/dispatch [::events/edit-db dissoc :spotify/user-playlists])
                                  (rf/dispatch [::events/edit-db dissoc :spotify/user-tracks])
                                  (rf/dispatch [::events/edit-db dissoc :spotify/user-artists])
                                  ;; (rf/dispatch [::events/init-panel :playlist])
                                  (spotify/download-user-data @token @debug?))}
     "Reload Spotify Data"]))

(defn create-playlist-button [{:keys [id genre name _label _exists? _count _target-count] :as _playlist}]
  (let [k            (keyword :create-playlist id)
        token        (rf/subscribe [::subs/db :spotify/token])
        loading      (rf/subscribe [::subs/db k])
        app-loading? (rf/subscribe [::subs/db nil app-loading?])
        track-ids    (rf/subscribe [::subs/db nil (genre-track-ids genre)])]
    [:> IconButton {:variant "plain"
                    :color   "success"
                    :onClick (fn [_]
                               (rf/dispatch
                                 [::events/http-request
                                  {:key       k
                                   :method    :post
                                   :uri       "https://api.spotify.com/v1/me/playlists"
                                   :headers   {:Authorization (str "Bearer " (:access-token @token))}
                                   :params    {:name          name
                                               :public        false
                                               :collaborative false
                                               :description   "Playlist auto generate by Spotify Tools."}
                                   :update-db (fn [db {:keys [id] :as result}]
                                                (-> db
                                                    (update-in [:spotify/user-playlists :items] assoc id result)
                                                    (update-in [:spotify/user-playlists :total] inc)))
                                   :update-fx (fn [fx {:keys [id] :as _result}]
                                                (into (or fx [])
                                                      (into []
                                                            (comp
                                                              (map #(str "spotify:track:" %))
                                                              (partition-all 100)
                                                              (map (fn [uris]
                                                                     [::events/http-request
                                                                      {:key       (keyword :update-playlist-button id)
                                                                       :method    :post
                                                                       :uri       (str "https://api.spotify.com/v1/playlists/" id "/tracks")
                                                                       :headers   {:Authorization (str "Bearer " (:access-token @token))}
                                                                       :params    {:uris uris}
                                                                       :update-db (fn [db _result]
                                                                                    (-> db
                                                                                        ;; TODO something is wrong with these updates
                                                                                        (update-in [:spotify/user-playlists :items id :tracks :total] + (count uris))
                                                                                        (update-in [:spotify/user-playlists :items id :tracks :items] (fn [tracks]
                                                                                                                                                        (into (or tracks #{})
                                                                                                                                                              (comp
                                                                                                                                                                (map #(str/replace % "spotify:track:" ""))
                                                                                                                                                                (map (fn [id] {:track {:id id}})))
                                                                                                                                                              uris)))))}]))
                                                              (map #(do [:dispatch %])))
                                                            @track-ids)))}]))}
     (if (or (= @loading :loading) @app-loading?)
       [:> CircularProgress {:color "success"}]
       [:> FavoriteBorderIcon]
       #_[:img {:src "https://developer.spotify.com/assets/branding-guidelines/heart-64.svg" :width 20}])]))

(defn update-playlist-button [{:keys [id genre _name _label _exists? _count _target-count] :as _playlist}]
  (let [k                  (keyword :update-playlist-button id)
        token              (rf/subscribe [::subs/db :spotify/token])
        loading            (rf/subscribe [::subs/db k])
        app-loading?       (rf/subscribe [::subs/db nil app-loading?])
        track-ids          (rf/subscribe [::subs/db nil (genre-track-ids genre)])
        existing-track-ids (rf/subscribe [::subs/db :spotify/user-playlists (fn [{:keys [items]}]
                                                                              (into #{}
                                                                                    (map #(-> % :track :id))
                                                                                    (:items (:tracks (get items id)))))])]
    [:> IconButton {:variant "plain"
                    :color   "primary"
                    :onClick (fn [_]
                               ;; TODO make sequential not parallel
                               (into []
                                     (comp
                                       (remove @existing-track-ids)
                                       (map #(str "spotify:track:" %))
                                       (partition-all 100)
                                       (map (fn [uris]
                                              [::events/http-request
                                               {:key       k
                                                :method    :post
                                                :uri       (str "https://api.spotify.com/v1/playlists/" id "/tracks")
                                                :headers   {:Authorization (str "Bearer " (:access-token @token))}
                                                :params    {:uris uris}
                                                :update-db (fn [db _result]
                                                             (-> db
                                                                 (update-in [:spotify/user-playlists :items id :tracks :total] + (count uris))
                                                                 (update-in [:spotify/user-playlists :items id :tracks :items] (fn [tracks]
                                                                                                                                 (into (or tracks #{})
                                                                                                                                       (comp
                                                                                                                                         (map #(str/replace % "spotify:track:" ""))
                                                                                                                                         (map (fn [id] {:track {:id id}})))
                                                                                                                                       uris)))))}]))
                                       (map #(rf/dispatch %)))
                                     @track-ids))}
     (if (or (= @loading :loading) @app-loading?)
       [:> CircularProgress {:color "primary"}]
       [:> SyncIcon])]))

(defn delete-playlist-button [{:keys [id _genre _name _label _exists? _count _target-count] :as _playlist}]
  (let [token        (rf/subscribe [::subs/db :spotify/token])
        loading      (rf/subscribe [::subs/db (keyword :delete-playlist id)])
        app-loading? (rf/subscribe [::subs/db nil app-loading?])]
    [:> IconButton {:variant "plain"
                    :color   "danger"
                    :onClick (fn [_]
                               (rf/dispatch
                                 (spotify/delete-playlist! @token id)))}
     (if (or (= @loading :loading) @app-loading?)
       [:> CircularProgress {:color "danger"}]
       [:> DeleteIcon])]))

(defn delete-all-playlists-button []
  (let [token                 (rf/subscribe [::subs/db :spotify/token])
        loading               (rf/subscribe [::subs/db :delete-all-playlists-button])
        playlists-loading     (rf/subscribe [::subs/db :playlists-request])
        genre-playlist-ids    (rf/subscribe [::subs/db :spotify/user-playlists #(not-empty
                                                                                  (into []
                                                                                        (map :id)
                                                                                        (genre-playlists %)))])
        genre-playlists-count (rf/subscribe [::subs/db :spotify/user-playlists (comp count genre-playlists)])]
    (when (pos? @genre-playlists-count)
      [:> Button {:variant        "plain"
                  :color          "danger"
                  :startDecorator (r/as-element [:> DeleteIcon])
                  :disabled       (or (= @loading :loading)
                                      (= @playlists-loading :loading))
                  :onClick        (fn [_]
                                    ;; TODO make sequential not parallel
                                    (doseq [playlist-id @genre-playlist-ids]
                                      (rf/dispatch (spotify/delete-playlist! @token playlist-id))))}
       (str "Delete All " @genre-playlists-count " Genre Playlists")])))

(def term (r/atom "")) ;; NOTE weird mix of atom and app-db, probably should use just app-db, I doubt there is much performance loss to app-db vs atom

(defn genre-searcher []
  [:> Input {:variant        "outlined"
             :placeholder    "Search Genres"
             :startDecorator (r/as-element
                               [:> SearchIcon {:sx {:fontSize "1.8rem"}}])
             :endDecorator   (when (not-empty @term)
                               (r/as-element
                                 [:> IconButton {:variant "plain"
                                                 :onClick #(do
                                                             (reset! term "")
                                                             (rf/dispatch [::events/edit-db dissoc :playlist-search-term]))}
                                  [:> ClearIcon {:sx {:fontSize "1.0rem"}}]]))
             :value          (or @term "")
             :onChange       (fn [^js e]
                               (let [new-term (.. e -target -value)]
                                 (reset! term new-term)
                                 (rf/dispatch [::events/debounce
                                               ::events/edit-db assoc
                                               :playlist-search-term
                                               new-term])))}])

(defn genre-list []
  (let [genre-playlists (rf/subscribe [::subs/db nil build-genre-playlists])
        term            (rf/subscribe [::subs/playlist-search-term])]
    (when (not-empty @genre-playlists)
      (into [:> List {#_#_:sx {:backgroundColor "white"
                               :width           "100%"}}]
            (comp
              (filter (fn [playlist]
                        (if (not-empty @term)
                          (str/includes? (str/lower-case (:label playlist)) (str/lower-case @term))
                          true)))
              (map (fn [{:keys [label id exists? count target-count] :as playlist}]
                     [:> ListItem {:endAction (r/as-element
                                                [:> Stack {:direction "row"}
                                                            (when-not exists?
                                                              [create-playlist-button playlist])
                                                            (when (and exists? (< count target-count))
                                                              [update-playlist-button playlist])
                                                            (when exists?
                                                              [delete-playlist-button playlist])])}
                      label]))
              (interpose [:> ListDivider]))
            @genre-playlists))))

;; TODO would be pretty cool if the list could be split into as many columns as the screen allows
(defn genre-playlist-maker []
  (let [loading? (rf/subscribe [::subs/db nil app-loading?])]
    (when (not @loading?)
      [:> Stack nil
       [genre-searcher]
       [genre-list]])))

(defn loading-progress-indicator []
  (let [playlists-count   (rf/subscribe [::subs/db :spotify/user-playlists count-items])
        playlists-total   (rf/subscribe [::subs/db :spotify/user-playlists (comp int :total)])
        tracks-count      (rf/subscribe [::subs/db :spotify/user-tracks count-items])
        tracks-total      (rf/subscribe [::subs/db :spotify/user-tracks (comp int :total)])
        artists-count     (rf/subscribe [::subs/db :spotify/user-artists count])
        artists-total     (rf/subscribe [::subs/db :spotify/user-tracks (comp count spotify/track-artists-set)])
        playlists-percent (if (zero? @playlists-total)
                            0
                            (min (js/Math.floor (* (/ @playlists-count @playlists-total) 100)) 100))
        tracks-percent    (if (zero? @tracks-total)
                            0
                            (min (js/Math.floor (* (/ @tracks-count @tracks-total) 100)) 100))
        artists-percent   (if (zero? @artists-total)
                            0
                            (min (js/Math.floor (* (/ @artists-count @artists-total) 100)) 100))]
    [:> Stack {:spacing 1}
     [:> Typography {:level "body1"}
      (str "Downloaded " @playlists-count " of " @playlists-total " playlists (" playlists-percent "%)")]
     [:> LinearProgress {:determinate true
                         :value   playlists-percent}]
     [:> Typography {:level "body1"}
      (str "Downloaded " @tracks-count " of " @tracks-total " tracks (" tracks-percent "%)")]
     [:> LinearProgress {:determinate true
                         :value   tracks-percent}]
     [:> Typography {:level "body1"}
      (str "Downloaded " @artists-count " of " @artists-total " artists (" (if (zero? artists-percent)
                                                                             "waiting for tracks...)"
                                                                             (str artists-percent "%)")))]
     [:> LinearProgress {:determinate true
                         :value   artists-percent}]]))

(defn download-spotify-data-button []
  (let [token  (rf/subscribe [::subs/db :spotify/token])
        debug? (rf/subscribe [::subs/db :debug?])]
    [:> Button {:sx      {:minWidth    "300px"
                          #_#_:padding "50px"}
                :onClick (fn [_]
                           (spotify/download-user-data @token @debug?))}
     "Click to Download Spotify Data"]))

(defn app [] ;; note that the entire app lives inside the card, no cards inside the app
  (let [token    (rf/subscribe [::subs/db :spotify/token])
        loading?  (rf/subscribe [::subs/db nil app-loading?])
        playlists (rf/subscribe [::subs/db :spotify/user-playlists some?])
        tracks    (rf/subscribe [::subs/db :spotify/user-tracks some?])
        artists   (rf/subscribe [::subs/db :spotify/user-artists some?])]
    (if @token
      (cond
        @loading?
        [loading-progress-indicator]
        (or (not @playlists) (not @tracks) (not @artists))
        [download-spotify-data-button]
        :else
        [genre-playlist-maker])
      [spotify/button])))

(defn summary []
  (let [playlists-count       (rf/subscribe [::subs/db :spotify/user-playlists count-items])
        genre-playlists-count (rf/subscribe [::subs/db :spotify/user-playlists (comp count genre-playlists)])
        tracks-count          (rf/subscribe [::subs/db :spotify/user-tracks count-items])
        artists-count         (rf/subscribe [::subs/db :spotify/user-artists count])]
    [:> Stack {:spacing    1
               :alignItems #_"flex-start" "center"}
     [:> Typography {:level "h4"} "You have saved..."]
     [:> Typography {:level "body1"}
      (str @playlists-count " playlists (of which " @genre-playlists-count " are genre-based)")]
     [:> Typography {:level "body1"}
      (str @tracks-count " tracks")]
     [:> Typography {:level "body1"}
      (str @artists-count " artists")]]))

(defn panel []
  (let [token    (rf/subscribe [::subs/db :spotify/token])
        loading? (rf/subscribe [::subs/db nil app-loading?])
        playlists (rf/subscribe [::subs/db :spotify/user-playlists some?])
        tracks    (rf/subscribe [::subs/db :spotify/user-tracks some?])
        artists   (rf/subscribe [::subs/db :spotify/user-artists some?])]
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [c/header {:title    "Genre Playlists"
                :subtitle "Generate a playlist from your liked songs based on their genres"}]
     [:> Stack {:spacing    4
                :alignItems "stretch"
                :sx         (cond-> {:alignSelf "center"
                                     :maxWidth  "800px"}
                              ;; TODO need to be screen-size aware
                              #_#_@loading? (assoc :minWidth "800px"))}
      (when (and @token (not @loading?) (or @playlists @tracks @artists))
        [:> Card {:variant "outlined"}
         [:> CardContent nil
          [summary]
          [:> Stack {:direction "row" :spacing 1}
           [reload-spotify-data-button]
           [delete-all-playlists-button]]]])
      (if @token
        [:> Card {:variant "outlined"
                  :sx {:width "100%"}}
         [:> CardContent nil
          [app]]]
        [spotify/button])]]))
