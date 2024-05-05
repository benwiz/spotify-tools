(ns benwiz.spotify-tools.views.search
  (:require
   ["@mui/icons-material/Clear" :default ClearIcon]
   ["@mui/icons-material/Add" :default AddIcon]
   ["@mui/icons-material/Remove" :default RemoveIcon]
   ["@mui/joy/Input" :default Input]
   ["@mui/icons-material/Equalizer" :default EqualizerIcon]
   ["@mui/joy/AspectRatio" :default AspectRatio]
   ["@mui/joy/ButtonGroup" :default ButtonGroup]
   ["@mui/joy/Button" :default Button]
   ["@mui/joy/IconButton" :default IconButton]
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/Slider" :default Slider]
   ["@mui/joy/TextField" :default TextField]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/CardOverflow" :default CardOverflow]
   ["@mui/joy/CircularProgress" :default CircularProgress]
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/IconButton" :default IconButton]
   ["@mui/joy/ListItemContent" :default ListItemContent]
   ["@mui/joy/ListItemDecorator" :default ListItemDecorator]
   [benwiz.spotify-tools.utils.links :as links]
   ["@mui/joy/Sheet" :default Sheet]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.spotify-api :as spotify-api]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [cljs-bean.core :refer [->clj]]
   [clojure.string :as str]
   [goog.object]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn discrete-control [k [min max] default]
  (let [v        (rf/subscribe [::subs/db k])
        on-click (fn [f]
                   (fn [_]
                     (rf/dispatch [::events/edit-db assoc k (f (or @v default))])))]
    [:> ButtonGroup {:orientation "vertical"}
     [:> IconButton {:onClick (on-click inc)} [:> AddIcon]]
     ;; [:> Button {:disabled true} (or @v default)]
     [:> Input {:variant  "outlined"
                :type "number"
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
     [:> IconButton {:onClick (on-click dec)} [:> RemoveIcon]]]))

(defn range-slider [[low-k min] [high-k max]]
  (let [low          (rf/subscribe [::subs/db low-k])
        high         (rf/subscribe [::subs/db high-k])
        on-change    (fn [k default]
                       (fn [^js e]
                         (let [new-v (.. e -target -value)]
                           (rf/dispatch [::events/edit-db assoc k (or new-v default)]))))]
    [:> Stack {:direction "row" :spacing 3}
     ;; TODO I need a label in here
     [discrete-control :search/low [min max] min]
     [:> Slider {:sx                #js {:flexGrow 1}
                 :track             false
                 :value             #js [(or @low min) (or @high max)]
                 :min               min
                 :max               max
                 :marks             #js [#js {:value min :label (str min)}
                                         (let [mid (/ max 2)] #js {:value mid :label (str mid)})
                                         #js {:value max :label (str max)}]
                 :getAriaLabel      (fn [] "Range Slider Name")
                 :valueLabelDisplay "auto" ;; "on" "off"
                 :onChange          (fn [^js _e [low high]]
                                      (rf/dispatch [::events/edit-db assoc :search/low (or low min)])
                                      (rf/dispatch [::events/edit-db assoc :search/high (or high max)]))}]
     [discrete-control :search/high [min max] max]]))

(defn app []
  (let []
    [:<>
     ;; [spotify/searcher]
     [range-slider
      [:search/low 10]
      [:search/high 90]]]))

(defn panel []
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [c/header {:title "Search and Filter"}]
     (if @token
       [:> Card {:variant "outlined"
                 :sx      {:alignSelf "center"
                           :maxWidth  "800px"
                           :width     "100%"}}
        [:> CardContent nil

         [app]]]
       [spotify/button])]))
