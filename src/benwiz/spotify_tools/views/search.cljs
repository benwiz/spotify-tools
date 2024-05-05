(ns benwiz.spotify-tools.views.search
  (:require
   ["@mui/icons-material/Add" :default AddIcon]
   ["@mui/icons-material/Remove" :default RemoveIcon]
   ["@mui/joy/Box" :default Box]
   ["@mui/joy/Button" :default Button]
   ["@mui/joy/ButtonGroup" :default ButtonGroup]
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/Grid" :default Grid]
   ["@mui/joy/IconButton" :default IconButton]
   ["@mui/joy/LinearProgress" :default LinearProgress]
   ["@mui/joy/List" :default List]
   ["@mui/joy/ListDivider" :default ListDivider]
   ["@mui/joy/ListItem" :default ListItem]
   ["@mui/joy/ListItemContent" :default ListItemContent]
   ["@mui/joy/ListItemDecorator" :default ListItemDecorator]
   ["@mui/joy/Slider" :default Slider]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [benwiz.spotify-tools.utils.spotify-api :as spotify-api]
   [clojure.string :as str]
   [goog.object]
   [re-frame.core :as rf]))

(defn discrete-control [k idx min max step default] ;; TODO this can be a lot smaller, maybe underneat the slider
  (let [v        (rf/subscribe [::subs/db k #(if (vector? %) (nth % idx) default)])
        on-click (fn [f]
                   (fn [_]
                     (rf/dispatch [::events/edit-db update k
                                   (fn [low-high]
                                     (update (or low-high [min max]) idx
                                             (fn [v] (if v (f v) default))))])))]
    [:> ButtonGroup {:orientation "vertical"}
     [:> IconButton {:onClick (on-click #(+ % step))} [:> AddIcon]]
     [:> Button {:disabled true :sx #js {:width "100%"}} (or @v default)]
     #_[:> Input {:variant  "outlined"
                  :type     "number"
                  :value    (or (str @v) default)
                  :onChange (fn [^js e]
                              (when-some [new-v (some-> (.. e -target -value) not-empty js/parseInt)]
                                (rf/dispatch [::events/edit-db assoc k
                                              new-v
                                              #_(cond ;; can't do this because I need to type the first digit of a large number
                                                  (> new-v max) max
                                                  (< new-v min) min
                                                  :else         new-v)])))
                  :sx       #js {:maxWidth "100px"}}]
     [:> IconButton {:onClick (on-click #(- % step))} [:> RemoveIcon]]]))

(defn range-slider [k [min max step]]
  (let [low-high   (rf/subscribe [::subs/db k])
        [low high] (or @low-high [min max])
        step       (or step 1)]
    [:> Stack {:spacing 1 :alignItems "center"}
     [:span nil (name k)]
     [:> Stack {:direction "row" :spacing 3
                :sx        #js {:width "100%"}}
      [discrete-control k 0 min max step min]
      [:> Slider {:sx                #js {:flexGrow 1}
                  :variant           "solid"
                  :track             "normal" ;; false, "normal", "inverted"
                  :value             #js [(or low min) (or high max)]
                  :min               min
                  :max               max
                  :step              step
                  #_#_:marks         #js [#js {:value min :label (str min)}
                                          (let [mid (/ max 2)] #js {:value mid :label (str mid)})
                                          #js {:value max :label (str max)}]
                  :getAriaLabel      (fn [] (name k))
                  :valueLabelDisplay "auto" ;; "on" "off" "auto"
                  :onChange          (fn [^js _e [low high]]
                                       (rf/dispatch [::events/edit-db assoc k [low high]]))}]
      [discrete-control k 1 min max step max]]]))

(def audio-feature-schema
  #_{:analysis_url     "https://api.spotify.com/v1/audio-analysis/2hWI9GNr3kBrxZ7Mphho4Q"
     :id               "2hWI9GNr3kBrxZ7Mphho4Q"
     :track_href       "https://api.spotify.com/v1/tracks/2hWI9GNr3kBrxZ7Mphho4Q"
     :type             "audio_features"
     :uri              "spotify:track:2hWI9GNr3kBrxZ7Mphho4Q"
     :danceability     0.803
     :duration_ms      337840
     :energy           0.814
     :acousticness     0.198
     :instrumentalness 1.15E-5
     :key              4
     :liveness         0.223
     :loudness         -4.579
     :mode             0
     :speechiness      0.0381
     :tempo            117.08
     :time_signature   4
     :valence          0.933}
  [:map
   [:id :string]
   [:uri :string]
   [:track_href :string]
   [:type :string]
   [:analysis_url :string]
   [:acousticness {:min 0 :max 1 :step 0.00010} :float]
   [:danceability {:min 0 :max 1 :step 0.001} :float]
   [:duration_ms {:min 0 :max (* 60 60 1000) :step 1000} :int] ;; max 1h
   [:energy {:min 0 :max 1 :step 0.001} :float]
   [:instrumentalness {:min 0 :max 1 :step 0.001} :float]
   [:key {:min -1 :max 11 :step 1} :int]
   [:liveness {:min 0 :max 1 :step 0.001} :float]
   [:loudness {:min -60 :max 0 :step 1} :float] ;; decibles, not strict min/max
   [:mode {:min 0 :max 1 :step 1} :int] ;; 0 or 1 only
   [:speechiness {:min 0 :max 1 :step 0.001} :float]
   [:tempo {:min 0 :max 300 :step 1} :float]
   [:time_signature {:min 3 :max 7 :step 1} :int]
   [:valence {:min 0 :max 1 :step 0.001} :float]])

(defn facets []
  (into [:> Grid {:container true :spacing 8}]
        (comp
          (remove #{:map})
          (filter #(#{:int :float} (last %)))
          (map (fn [[k {:keys [min max step]} t]]
                 [:> Grid {:xs 12 :sm 6}
                  [range-slider (keyword :search k) [min max step]]])))
        audio-feature-schema))

(defn filter-audio-features [db]
  (let [parameters
        (into {}
              (comp
                (remove #{:map})
                (filter #(#{:int :float} (last %)))
                (map first)
                (map #(keyword :search %))
                (map #(do [% (get db %)]))
                (filter second)
                (map (fn [[k v]]
                       [(keyword (name k)) v])))
              audio-feature-schema)]
    (->> (:items (:spotify/audio-features db))
         (into []
               (comp
                 (remove (fn [[_id features]] ;; remove if one failure
                           ;; later can use malli to compare, I think
                           (some (fn [[k v]]
                                   (when-some [param (get parameters k)]
                                     (let [[min max] param]
                                       (cond
                                         (< v min) :FAIL
                                         (> v max) :FAIL))))
                                 features)))
                 (map second)
                 (map (fn [feature] ;; calculate diff for sorting... TODO this is very, very naive
                        (let [diff (fn [features]
                                     (reduce (fn [acc [k v]]
                                               (if (#{:duration_ms} k) ;; skip
                                                 acc
                                                 (if-some [param (get parameters k)]
                                                   (+ acc
                                                      (let [[min max] param
                                                            mid       (+ (/ (- max min) 2) min)]
                                                        (abs (- v mid))))
                                                   acc)))
                                             0
                                             features))]
                          (assoc feature :diff (diff feature)))))))
         (sort-by :diff)
         (into []))))

(defn list-item [option]
  [:> ListItem nil
   [:> ListItemDecorator nil
    [:img {:src     (:url (last (:images (:album option))))
           :width   40
           :loading "lazy"}]]
   [:> ListItemContent {:sx {:marginLeft "1.0rem"}}
    [:> Typography {:level "body1"} (:name option)]
    [:> Typography {:level "body2"} (str/join ", " (mapv :name (:artists option)))]
    [:> Typography {:level "body2"} (:diff option)]]])

(defn- ->hashmap [coll] (into {} (map (juxt (comp :id :track) identity)) (:items coll)))

(defn results []
  (let [tracks   (rf/subscribe [::subs/db :spotify/user-tracks ->hashmap])
        features (rf/subscribe [::subs/db nil filter-audio-features])]
    (into [:> List nil]
          (comp
            (take 100)
            (map (fn [feature]
                   [list-item (assoc (:track (get @tracks (:id feature)))
                                     :diff (:diff feature))]))
            (interpose [:> ListDivider]))
          @features)))

(defn download-spotify-data-button []
  (let [token  (rf/subscribe [::subs/db :spotify/token])
        debug? (rf/subscribe [::subs/db :debug?])]
    [:> Button {:sx      {:minWidth    "300px"
                          #_#_:padding "50px"}
                :onClick (fn [_]
                           (spotify-api/download-user-tracks @token @debug?))}
     "Click to Download Spotify Data"]))

(defn- count-items [v] (count (:items v)))

(defn loading-progress-indicator []
  (let [tracks-count           (rf/subscribe [::subs/db :spotify/user-tracks count-items])
        tracks-total           (rf/subscribe [::subs/db :spotify/user-tracks (comp int :total)])
        tracks-percent         (if (zero? @tracks-total)
                                 0
                                 (min (js/Math.floor (* (/ @tracks-count @tracks-total) 100)) 100))
        audio-features-count   (rf/subscribe [::subs/db :spotify/audio-features count-items])
        audio-features-percent (if (zero? @tracks-total) ;; yes tracks-total
                                 0
                                 (min (js/Math.floor (* (/ @audio-features-count @tracks-total) 100)) 100))]
    [:> Stack {:spacing 1}
     [:> Typography {:level "body1"}
      (str "Downloaded " @tracks-count " of " @tracks-total " tracks (" tracks-percent "%)")]
     [:> LinearProgress {:determinate true
                         :value       tracks-percent}]
     [:> Typography {:level "body1"}
      (str "Downloaded " @audio-features-count " of " @tracks-total " tracks (" audio-features-percent "%)")]
     [:> LinearProgress {:determinate true
                         :value       audio-features-percent}]]))

(defn content []
  [:> Grid {:container true :spacing 4}
   [:> Grid {:sx 12 :sm 8}
    [:> Card {:variant "outlined"}
     [:> CardContent nil [facets]]]]
   [:> Grid {:sx 12 :sm 4}
    [:> Card {:variant "outlined"}
     [:> CardContent nil [results]]]]])

(defn- app-loading? [db]
  (or (= (:tracks-request db) :loading)))

(defn app []
  (let [token          (rf/subscribe [::subs/db :spotify/token])
        loading?       (rf/subscribe [::subs/db nil app-loading?])
        tracks         (rf/subscribe [::subs/db :spotify/user-tracks some?])
        audio-features (rf/subscribe [::subs/db :spotify/audio-features some?])]
    (if @token
      (cond
        @loading?
        [:> Card {:variant "outlined"}
         [:> CardContent nil
          [loading-progress-indicator]]]
        (or (not @tracks) (not @audio-features))
        [download-spotify-data-button]
        :else
        [content])
      [spotify/button])))

(defn panel []
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [c/header {:title "Search and Filter"}]
     (if @token
       [:> Box {:sx {:alignSelf "center"
                       :maxWidth  "1200px"
                       :width     "100%"}}
        [app]]
       [spotify/button])]))
