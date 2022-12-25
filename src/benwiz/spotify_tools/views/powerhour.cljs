(ns benwiz.spotify-tools.views.powerhour
  (:require
   ["@mui/joy/Button" :default Button]
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.links :as links]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [clojure.string :as str]
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as rf]))

;; Helper fns

(defn device-choices [devices]
  (into []
        (comp
          (map #(assoc % :label (str (:name %) " (" (:type %) ")")))
          (map #(select-keys % [:id :is_active :label])))
        (sort-by (juxt :type :name) devices)))

(defn selected-device [devices]
  (first
    (into []
          (comp
            (filter :is_active)
            (take 1))
          devices)))

;; Components

(defn title []
  [re-com/title
   :src   (at)
   :label "Power Hour Remote Control"
   :level :level1])

(defn subtitle []
  [re-com/v-box
   :src (at)
   :children
   [[re-com/title
     :src   (at)
     :label "Automatically skip the song every minute."
     :level :level2]
    [re-com/title
     :src   (at)
     :label "You must have the Spotify app open on your computer, phone, or some other device to play the music. Ensure that your queue is populated with the songs you want to hear."
     :level :level2]]])

(defn disclaimer []
  [re-com/v-box
   :src (at)
   :children
   [[re-com/title
     :src   (at)
     :label "New implementation coming soon, view old page for current version"
     :level :level3]
    [re-com/hyperlink-href
     :src    (at)
     :label  "Old Power Hour implementation"
     :href   "https://benwiz.com/powerhour"
     :target "_blank"]]])

(defn device-select [] ;; might be better as radio buttons
  (let [token    (rf/subscribe [::subs/db :spotify/token])
        loading? (rf/subscribe [::subs/db :transfer-playback #(= % :loading)])
        devices  (rf/subscribe [::subs/db :spotify/devices device-choices])
        selected (rf/subscribe [::subs/db :spotify/devices selected-device])]
    [re-com/single-dropdown
     :src         (at)
     :style       {:align-self "center"}
     :choices     @devices
     :model       (:id @selected)
     :title?      true
     :disabled?   @loading?
     :placeholder "Choose a device"
     :width       "300px"
     :max-height  "400px"
     :filter-box? false
     :repeat-change? true
     :on-drop     #(rf/dispatch (spotify/get-devices @token)) ;; shouldn't be necessary since I have an interval on it, but can't hurt
     :on-change   (fn [id]
                    (rf/dispatch (spotify/transfer-playback! @token id)))]))

(defn interval-select []
  (let [interval (rf/subscribe [::subs/db :powerhour/interval])]
    [re-com/single-dropdown
     :src         (at)
     :style       {:align-self "center"}
     :choices     [{:label "10 seconds" :value 10}
                   {:label "30 seconds" :value 30}
                   {:label "60 seconds" :value 60}
                   {:label "90 seconds" :value 90}
                   {:label "120 seconds" :value 120}]
     :model       @interval
     :id-fn       :value
     :title?      true
     :placeholder "Choose an interval"
     :width       "300px"
     :max-height  "400px"
     :filter-box? false
     :repeat-change? true
     :on-change   (fn [value]
                    (rf/dispatch [::events/edit-db #(assoc % :powerhour/interval value)]))]))

(defn duration-select []
  (let [duration (rf/subscribe [::subs/db :powerhour/duration])]
    [re-com/single-dropdown
     :src         (at)
     :style       {:align-self "center"}
     :choices     [{:label "15 minutes" :value 15}
                   {:label "30 minutes" :value 30}
                   {:label "60 minutes" :value 60}
                   {:label "90 minutes" :value 90}]
     :model       @duration
     :id-fn       :value
     :title?      true
     :placeholder "Choose a duration"
     :width       "300px"
     :max-height  "400px"
     :filter-box? false
     :repeat-change? true
     :on-change   (fn [value]
                    (rf/dispatch [::events/edit-db #(assoc % :powerhour/duration value)]))]))

(defn album-cover []
  (let [spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])
        url              (rf/subscribe [::subs/db :spotify/playback-state #(-> % :item :album :images second :url)])]
    (when (and @spotify-playing? @url)
      [:img {:src    @url
             :height 300
             :width  300}])))

(defn track-title []
  (let [spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])
        title            (rf/subscribe [::subs/db :spotify/playback-state #(-> % :item :name)])]
    (when (and @spotify-playing? @title)
      [re-com/label
       :src (at)
       :label (str "Track: " @title)])))

(defn album-title []
  (let [spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])
        title            (rf/subscribe [::subs/db :spotify/playback-state #(-> % :item :album :name)])]
    (when (and @spotify-playing? @title)
      [re-com/label
       :src (at)
       :label (str "Album: " @title)])))

(defn artist-name []
  (let [spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])
        title            (rf/subscribe [::subs/db :spotify/playback-state
                                        #(->> % :item :artists
                                              (into []
                                                    (map :name))
                                              (str/join ", "))])]
    (when (and @spotify-playing? @title)
      [re-com/label
       :src (at)
       :label (str "Artist: " @title)])))

(defn clock [seconds]
  (when seconds
    (let [minutes (js/Math.floor (/ seconds 60))
          seconds (- seconds (* minutes 60))]
      (str (.padStart (js/String minutes) 2 "0")
           ":"
           (.padStart (js/String seconds) 2 "0")))))

(defn timer []
  (let [timer (rf/subscribe [::subs/db :powerhour/timer clock])]
    (when @timer
      [re-com/label
       :src   (at)
       :label @timer])))

(defn play-button []
  (let [token            (rf/subscribe [::subs/db :spotify/token])
        loading?         (rf/subscribe [::subs/db :spotify-play #(= % :loading)])
        spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])
        playing?         (rf/subscribe [::subs/db :powerhour/playing])
        interval         (rf/subscribe [::subs/db :powerhour/interval])
        duration         (rf/subscribe [::subs/db :powerhour/duration])]
    (when-not @playing?
      [re-com/button
       :src      (at)
       :label    "Play"
       :disabled? @loading?
       :on-click (fn []
                   (rf/dispatch [::events/edit-db assoc :powerhour/playing true])
                   (rf/dispatch [::events/edit-db update :powerhour/timer #(or % (* @duration 60))]) ;; just a failsafe, not really necessary, doesn't really matter when it happens in relation to the following dispatches either, which is nice
                   (when-not @spotify-playing?
                     (rf/dispatch [::events/http-request
                                   {:key     :spotify-play
                                    :method  :put
                                    :uri     "https://api.spotify.com/v1/me/player/play"
                                    :headers {:Authorization (str "Bearer " (:access-token @token))}
                                    :params  {:position_ms 0}}]))
                   (rf/dispatch [::events/interval
                                 {:id        :powerhour/timer
                                  :action    :start
                                  :frequency 1000
                                  :events    [[::events/edit-db update :powerhour/timer dec]]}])
                   (rf/dispatch [::events/register-track
                                 {:id           :powerhour/skip-to-next
                                  :subscription [::subs/db :powerhour/timer #(zero? (mod % @interval))]
                                  :event-fn     (fn [skip?]
                                                  (when skip?
                                                    (assoc-in
                                                      (spotify/skip-to-next! @token)
                                                      [1 :update-fx]
                                                      (fn [fx _result]
                                                        (into fx
                                                              [[:dispatch (spotify/seek-to-position! @token 20000)]])))))}]))])))

(defn pause-button []
  (let [token            (rf/subscribe [::subs/db :spotify/token])
        loading?         (rf/subscribe [::subs/db :spotify-pause #(= % :loading)])
        playing?         (rf/subscribe [::subs/db :powerhour/playing])
        spotify-playing? (rf/subscribe [::subs/db :spotify/playback-state :is_playing])]
    (when @playing?
      [re-com/button
       :src      (at)
       :label    "Pause"
       :disabled? @loading?
       :on-click (fn []
                   (rf/dispatch [::events/edit-db assoc :powerhour/playing false])
                   (when spotify-playing?
                     (rf/dispatch [::events/http-request
                                   {:key     :spotify-pause
                                    :method  :put
                                    :uri     "https://api.spotify.com/v1/me/player/pause"
                                    :headers {:Authorization (str "Bearer " (:access-token @token))}}]))
                   (rf/dispatch [::events/interval {:id :powerhour/timer :action :end}])
                   (rf/dispatch [::events/dispose-track :powerhour/skip-to-next]))])))

(defn reset-button []
  (let [timer    (rf/subscribe [::subs/db :powerhour/timer])
        duration (rf/subscribe [::subs/db :powerhour/duration])
        target   (* @duration 60)]
    (when (and (not= @timer target) (some? @timer))
      [re-com/button
       :src      (at)
       :label    "Reset Game"
       :on-click (fn []
                   (rf/dispatch [::events/edit-db assoc :powerhour/playing false])
                   ;; reset timer
                   (rf/dispatch
                     [::events/interval
                      {:id     :powerhour/timer
                       :action :end
                       :update-fx
                       #(into % [[:dispatch
                                  [::events/edit-db assoc :powerhour/timer target]]])}])
                   ;; stop timer
                   (rf/dispatch [::events/interval {:id :powerhour/timer :action :end}])
                   ;; stop skipping
                   (rf/dispatch [::events/dispose-track :powerhour/skip-to-next]))])))

(defn start-app-button []
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    [:> Button {:sx      {:minWidth    "300px"
                          #_#_:padding "50px"}
                :onClick (fn [_]
                           (rf/dispatch (spotify/get-devices @token))
                           (rf/dispatch [::events/interval
                                         {:id        :devices
                                          :action    :start
                                          :frequency 5000
                                          :events    [(update (spotify/get-devices @token) 1 dissoc :key)]}])
                           (rf/dispatch (spotify/get-playback-state @token))
                           (rf/dispatch [::events/interval
                                         {:id        :playback-state
                                          :action    :start
                                          :frequency 1000
                                          :events    [(update (spotify/get-playback-state @token) 1 dissoc :key)]}]))}
     "Click to Load App"]))

(defn app []
  (let [token   (rf/subscribe [::subs/db :spotify/token])
        devices (rf/subscribe [::subs/db :spotify/devices])]
    (cond
      (not @token)
      [spotify/button]
      ;; (empty? @devices)
      ;; [start-app-button]
      :else
      [:> Stack {:spacing    2
                 :alignItems "center"}
       [device-select]
       [interval-select]
       [duration-select]
       [album-cover]
       [track-title]
       [album-title]
       [artist-name]
       [timer]
       [play-button]
       [pause-button]
       [reset-button]])))

(defn panel []
  (let [token    (rf/subscribe [::subs/db :spotify/token])
        interval (rf/subscribe [::subs/db :powerhour/interval])]
    [:> Stack {:spacing 2}
     [c/header {:title    "Power Hour"
                :subtitle (str "Change the song every " @interval " seconds")}]
     (if @token
       [:> Card {:variant "outlined"
                 :sx      {:alignSelf "center"
                           :maxWidth  "800px"}}
        [:> CardContent nil
         [app]]]
       [spotify/button])]))
