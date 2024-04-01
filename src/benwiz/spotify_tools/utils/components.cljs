(ns benwiz.spotify-tools.utils.components
  (:require
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.links :as links]
   [benwiz.spotify-tools.views.spotifylogin :as spotifylogin]
   [re-frame.core :as rf]))

(defn header [{:keys [title subtitle]}]
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    (prn 't token)
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [:> Stack {:direction      "row"
                :justifyContent "space-between"}
      [:> Typography {:level "h1"} title]
      [:> Stack {:direction "row"}
       (if @token
         [spotifylogin/logout-button]
         [spotifylogin/login-button])
       [links/home]]]
     (when subtitle
       [:> Typography {:level     "h5"
                       :textColor "neutral.500"}
        subtitle])
     [links/all]
     [:> Divider]]))

#_(defn footer []
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [:> Divider]])
