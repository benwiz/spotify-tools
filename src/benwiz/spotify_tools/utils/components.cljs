(ns benwiz.spotify-tools.utils.components
  (:require
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.utils.links :as links]))

(defn header [{:keys [title subtitle]}]
  [:> Stack {:spacing    2
             :alignItems "stretch"}
   [:> Typography {:level "h1"} title]
   (when subtitle
     [:> Typography {:level     "h5"
                     :textColor "neutral.500"}
      subtitle])
   [links/all]
   [:> Divider]])
