(ns benwiz.spotify-tools.events
  (:require
   [ajax.core :as ajax]
   [benwiz.spotify-tools.config :as config]
   [benwiz.spotify-tools.db :as db]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [district0x.re-frame.window-fx :as window]
   [re-frame.core :as rf]
   [troglotit.re-frame.debounce-fx]
   [vimsical.re-frame.fx.track :as track]))

(rf/reg-event-db
  ::initialize-db
  (fn-traced
    [_ _]
    (into db/default-db
          {:spotify/token          (when-some [token (edn/read-string (.getItem (.-localStorage ^js js/window) "spotify-token"))]
                                     (spotify/valid-token! token))
           #_#_:spotify/user-artists   (edn/read-string (.getItem (.-localStorage ^js js/window) "spotify/user-artists"))
           #_#_:spotify/user-tracks    (when-some [info (edn/read-string (.getItem (.-localStorage ^js js/window) "spotify/user-tracks"))]
                                     {:total (:total info)
                                      :items (into #{}
                                                   (mapcat (fn [idx]
                                                             (edn/read-string (.getItem (.-localStorage ^js js/window) (str "spotify/user-tracks." idx)))))
                                                   (range (:indices info)))})
           #_#_:spotify/user-playlists (edn/read-string (.getItem (.-localStorage ^js js/window) "spotify/user-playlists"))
           })))

;; this is intended for generic side effects but should be used sparingly
(rf/reg-event-fx
  ::side-effect
  (fn
    [_ [_ f]]
    (f)
    nil))

(rf/reg-event-db
  ::edit-db
  (fn-traced
    [db [_ f & args]]
    (apply f db args)))

(rf/reg-event-fx
  ::db-aware-event
  (fn
    [{:keys [db]} [_ f]]
    (f db)))

(rf/reg-event-fx
  ::set-local-store
  (fn
    [{:keys [db]} [_ k v]]
    (.setItem ^js (.-localStorage ^js js/window)
              k
              (if (fn? v)
                (v db)
                v))
    nil))

(rf/reg-event-fx
  ::remove-local-store
  (fn
    [_ [_ k]]
    (.removeItem ^js (.-localStorage ^js js/window) k)))

(rf/reg-event-fx
  ::navigate
  (fn-traced
    [_ [_ handler]]
    {:navigate handler}))

(rf/reg-event-fx
  ::set-active-panel
  (fn-traced
    [{:keys [db]} [_ active-panel]]
    {:db (assoc db :active-panel active-panel)}))

(rf/reg-event-fx
  ::set-spotify-token
  (fn-traced
    [{:keys [db]} [_ spotify-token route-handler]]
    {:db (assoc db :spotify/token spotify-token)
     :fx [[:dispatch [::set-local-store "spotify-token" spotify-token]]
          [:dispatch [::navigate route-handler]]]}))

(rf/reg-event-fx
  ::delete-spotify-token
  (fn-traced
    [{:keys [db]} [_]]
    {:db (dissoc db :spotify/token)
     :fx [[:dispatch [::remove-local-store "spotify-token"]]]}))

(rf/reg-event-fx ;; TODO probably should be parameterized with a function or something rather than the panel's identifier
  ::init-panel
  (fn-traced
    [{:keys [db]} [_ panel]]
    (when-some [token (:spotify/token db)]
      (case panel
        :powerhour    (let [devices (:spotify/devices db)]
                        (when (empty? devices)
                          {:fx [[:dispatch (spotify/get-devices token)]
                                [:dispatch [::interval
                                            {:id        :devices
                                             :action    :start
                                             :frequency 5000
                                             :events    [(update (spotify/get-devices token) 1 dissoc :key)]}]]
                                [:dispatch (spotify/get-playback-state token)]
                                [:dispatch [::interval
                                            {:id        :playback-state
                                             :action    :start
                                             :frequency 1000
                                             :events    [(update (spotify/get-playback-state token) 1 dissoc :key)]}]]]}))
        :analysis     (let [devices (:spotify/devices db)]
                        (when (empty? devices)
                          {:fx [[:dispatch (spotify/get-playback-state token)]
                                [:dispatch [::interval
                                            {:id        :playback-state
                                             :action    :start
                                             :frequency 1000
                                             :events    [(update (spotify/get-playback-state token) 1 dissoc :key)]}]]]}))
        ;; Rather than autoloading data I wrapped the event dispatches under a button
        ;; Leave this here though, this recursive init-panel pattern is useful
        #_#_:playlist (let [playlists (:spotify/user-playlists db)
                            tracks    (:spotify/user-tracks db)
                            artists   (:spotify/user-artists db)]
                        ;; (prn (mapv (comp some? not-empty) [playlists tracks artists]))
                        (some->>
                          (cond-> []
                            (empty? playlists)
                            (conj [:dispatch (spotify/get-users-playlists :playlists-request token)])
                            (empty? tracks)
                            (conj [:dispatch (let [get-users-tracks
                                                   (spotify/get-users-tracks :tracks-request token
                                                                             (if (:debug? db)
                                                                               config/debug-track-limit
                                                                               false))
                                                   options (second get-users-tracks)]
                                               (update-in get-users-tracks [1 :update-fx]
                                                          (fn [update-fx]
                                                            (let [f (fn [fx result]
                                                                      ;; when completely done
                                                                      (when (nil? ((:next options) options result))
                                                                        (into (or fx [])
                                                                              [[:dispatch [::init-panel panel]]])))]
                                                              (if update-fx
                                                                (comp f update-fx)
                                                                f)))))])
                            (and (empty? artists) (not-empty tracks) (not= (:tracks-request db) :loading))
                            (conj [:dispatch (spotify/get-artists token (spotify/track-artists-set tracks))]))
                          not-empty
                          (assoc {} :fx)))
        nil))))


(rf/reg-event-fx
  ::http-request
  (fn-traced
    [{:keys [db]} [_ {:keys [key method uri headers params url-params] :as options}]]
    {:db         (if key
                   (assoc db key :loading)
                   db)
     :http-xhrio {:method          method
                  :uri             uri
                  :headers         headers
                  :params          params
                  :url-params      url-params
                  :timeout         8000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  ;; :on-request      [::track-request-progress]
                  :on-success      [::success-http-result options]
                  :on-failure      [::failure-http-result options]}}))

#_(rf/reg-event-fx
  ::track-request-progress
  (fn
    [{:keys [db]} [_ xhrio]]
    (js/console.log xhrio)
    ))

(rf/reg-event-fx
  ::success-http-result
  (fn
    [{:keys [db]} [_ {:keys [key method uri update-db update-fx next] :as options
                      :or   {next (constantly nil)}} result]]
    ;; The `next` concept is useful because I can use it in both :db and :fx.
    ;; TODO however, it's probably better to eliminate it, build fx first, then
    ;; in db look for ::http-request events that share the same :key so know if
    ;; to dissoc the loading state key or not.
    {:db (cond-> db
           update-db
           (update-db result)
           (not update-db)
           (assoc-in [:http-request method uri] result)
           (nil? (next options result))
           (dissoc key))
     :fx (cond-> []
           update-fx
           (update-fx result db)
           (next options result)
           (into [[:dispatch [::http-request (next options result)]]]))}))

(rf/reg-event-fx
  ::failure-http-result
  (fn
    [{:keys [db]} [_ {:keys [key method uri] :as options} {:keys [status] :as result}]]
    (cond
      (<= 200 status 299) ;; idk why I'm getting  a 200 in here for DELETE playlist/playlist_id/followers
      {:fx [[:dispatch [::success-http-result options result]]]}
      (= 429 status) ;; TODO I can't find the retry-after header, so just waiting one second
      (do
        (js/console.log "Received a 429, retrying for reattempt number" (inc (:attempts options)))
        {:fx [[:dispatch-later {:ms 1000 :dispatch [::http-request (update options :attempts inc)]}]]})
      :else
      (do
        (cljs.pprint/pprint {::failure-http-result result
                             :method               method
                             :uri                  uri})
        {:db (-> db
                 (assoc-in [:http-request method uri] result)
                 (dissoc key))}))))

;; Debounce

(rf/reg-event-fx
  ::debounce
  (fn [fx [_ event & args]]
    {:dispatch-debounce {:key   event
                         :event (into [event] args)
                         :delay 200}}))

;; Intervals

;; Modified from: https://github.com/day8/re-frame/blob/master/docs/FAQs/PollADatabaseEvery60.md#a-side-note-about-effect-handlers-and-figwheel
(defonce live-intervals (atom {}))

(defn interval-handler [{:keys [action id frequency events]}]
  (condp = action
    :clean (doseq [k (keys @live-intervals)] ;; clear all existing intervals
             (interval-handler {:id k :action :end}))
    :start (swap! live-intervals update id
                  (fn [interval]
                    (or interval ;; don't create a new interval if one already exists at this id
                        (js/setInterval (fn []
                                          (doseq [event events]
                                            (rf/dispatch event)))
                                        frequency))))
    :end   (do
             (js/clearInterval (get @live-intervals id))
             (swap! live-intervals dissoc id))))

(rf/reg-fx :interval interval-handler)

(rf/reg-event-fx
  ::interval
  (fn-traced
    [_ [_ options]]
    (cond-> {:interval options}
      (:update-fx options)
      (update :fx (:update-fx options)))))

;; Events dispatched by changing subscriptions
;; (I'm not sold on this pattern yet, but I chose it
;; rather than hoping than managing many setInterval.)
;; Regardless, the implementation is very simple and
;; boils down to `reagent.ratom/track!`.
;; See: https://github.com/den1k/re-frame-utils/blob/master/src/vimsical/re_frame/fx/track.cljc#L68

(rf/reg-event-fx
  ::register-track
   (fn-traced
    [_ [_ options]]
    {::track/register options}))

(rf/reg-event-fx
  ::dispose-track
  (fn-traced
    [_ [_ id]]
    {::track/dispose {:id id}}))

;; Window resize events

(rf/reg-event-fx
  ::init-window-events
  (fn [_ [_ _]]
    {:window/on-resize {:dispatch    [::window-resized]
                        :debounce-ms 200}}))

(rf/reg-event-db
  ::window-resized
  (fn-traced
    [db [_ width]]
    (assoc db
           :window/width width
           :window/breakpoint (cond
                                (< width 700) :mobile
                                :else         :desktop))))
