(ns benwiz.spotify-tools.views.wwoz-to-spotify
  (:require
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.links :as links]
   [re-com.core :as re-com :refer [at]]))

;; TODO can upgrade to use https://developer.spotify.com/documentation/embeds/guides/using-the-iframe-api/
;; will allow more customization if I ever expand this tool to beyond wwoz

(defn github-link []
  [re-com/hyperlink-href
   :src    (at)
   :label  "View on GitHub"
   :href   "https://github.com/benwiz/wwoz_to_spotify"
   :target "_blank"])

;; (def iframe "<iframe style=\"border-radius:12px\" src=\"https://open.spotify.com/embed/playlist/5P6WEbhcUsmXB08owijHYd?utm_source=generator\" width=\"100%\" height=\"380\" frameBorder=\"0\" allowfullscreen=\"\" allow=\"autoplay)  ; clipboard-write; encrypted-media; fullscreen; picture-in-picture\"></iframe>")

(defn embedded-player []
  ;; [:div {:dangerouslySetInnerHTML {:__html iframe}}]
  [:div
   [:iframe
    {:style           {:border-radius "12px"
                       :margin        "10 auto"}
     :src             "https://open.spotify.com/embed/playlist/5P6WEbhcUsmXB08owijHYd?utm_source=generator"
     :width           "100%"
     :height          "500px"
     :frameBorder     "0"
     :allowFullScreen ""
     :allow           "autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"}]])

(defn panel []
  [:> Stack {:spacing 2}
   [c/header {:title    "WWOZ Radio on Spotify"
              :subtitle "A curated playlist of the last 100 songs played by WWOZ"}]
   [embedded-player]])
