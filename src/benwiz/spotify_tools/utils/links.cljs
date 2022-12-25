(ns benwiz.spotify-tools.utils.links
  (:require
   ["@mui/icons-material/ArrowBack" :default ArrowBackIcon]
   ["@mui/icons-material/Home" :default HomeIcon]
   ["@mui/joy/Chip" :default Chip]
   ["@mui/joy/Grid" :default Grid]
   ["@mui/joy/Link" :default Link]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn back []
  [:> Chip {:slotProps      {:action {:component "a" :href "#/"}} ;; TODO should use routing to get href
            :startDecorator (r/as-element [:> ArrowBackIcon {:sx {:color "black"}}])}
   "Back"])

(defn home []
  [:> Chip {:slotProps      {:action {:component "a" :href "https://benwiz.com"
                                      ;; only because routing is weird and won't open smoothly in same tab
                                      :target "_blank"}}
            :startDecorator (r/as-element [:> HomeIcon {:sx {:color "black"}}])}
   "Home"])

(defn github []
  [:> Chip {:slotProps      {:action {:component "a" :href "https://github.com/benwiz/spotify-tools"}}
            :startDecorator (r/as-element [:img {:src   "assets/github-mark.svg"
                                                 :width 20}])}
   "View on GitHub"])

(defn wwoz-github []
  [:> Chip {:slotProps      {:action {:component "a" :href "https://github.com/benwiz/wwoz_to_spotify"}}
            :startDecorator (r/as-element [:img {:src   "assets/github-mark.svg"
                                                 :width 20}])}
   "View on GitHub"])

(defn spotify-api []
  [:> Chip {:slotProps      {:action {:component "a" :href "https://developer.spotify.com/documentation/web-api/"}}
            :startDecorator (r/as-element [:img {:src   "assets/Spotify_Icon_RGB_Black.png"
                                                 :width 20}])}
   "Built with Spotify Web API"])

(defn all [] ;; TODO they need to break lines, not truncate. May need to use Grid instead of Stack.
  (let [active-panel (rf/subscribe [::subs/db :active-panel])]
    [:> Grid {:container true
              :direction "row"
              :spacing   1}
     (when (not= :home @active-panel)
       [:> Grid nil
        [back]])
     [:> Grid nil
      [home]]
     (if (= :wwoz @active-panel)
       [:> Grid nil
        [wwoz-github]]
       [:> Grid nil
        [github]])
     [:> Grid nil
      [spotify-api]]
     ;; TODO settings
     ;; TODO about
     ]))
