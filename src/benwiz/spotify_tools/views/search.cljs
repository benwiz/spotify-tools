(ns benwiz.spotify-tools.views.search
  (:require
   ["@mui/icons-material/Clear" :default ClearIcon]
   ["@mui/icons-material/Equalizer" :default EqualizerIcon]
   ["@mui/joy/AspectRatio" :default AspectRatio]
   ["@mui/joy/Autocomplete" :default Autocomplete]
   ["@mui/joy/AutocompleteOption" :default AutocompleteOption]
   ["@mui/joy/Card" :default Card]
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
           "hi"
           [spotify/searcher]]]
       [spotify/button])]))
