(ns benwiz.spotify-tools.views.home
  (:require
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/CardCover" :default CardCover]
   ["@mui/joy/Chip" :default Chip]
   ["@mui/joy/Divider" :default Divider]
   ["@mui/joy/Grid" :default Grid]
   ["@mui/joy/Link" :default Link]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.links :as links]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn card [{:keys [label panel img-src]}]
  >[:> Card { ;; :variant "outlined"
            :sx      {:textDecoration "none"
                      "&:hover"       {:boxShadow "md" :borderColor "neutral.outlinedHoverBorder"
                                       "& .cover" {:background "linear-gradient(to top, rgba(0,0,0,0.4), rgba(0,0,0,0) 200px), linear-gradient(to top, rgba(0,0,0,0.4), rgba(0,0,0,0) 300px)"}}
                      :height         "clamp(100px, 33vw, 300px)"}
            :onClick #(rf/dispatch [::events/navigate panel])}
   [:> CardCover nil
    [:img {:src img-src :loading "lazy"}]]
   [:> CardCover {:className "cover"
                  :sx        {:background "linear-gradient(to top, rgba(0,0,0,0.4), rgba(0,0,0,0) 200px), linear-gradient(to top, rgba(0,0,0,0.8), rgba(0,0,0,0) 300px)"}}]
   [:> CardContent {:sx {:justifyContent "flex-end"}}
    [:> Typography {:level      "h3"
                    :fontWeight "lg"
                    :textColor  "white"}
     label]]])

(defn panel []
  [:> Stack {:spacing 2}
   [c/header {:title "Spotify Tools"
              #_#_:subtitle "A collection of tools to augment Spotify"}]
   [:> Grid {:container true
             :spacing   2
             :alignSelf "center"}
    [:> Grid {:xs   12
              :sm   6}
     [card {:label   "Genre Playlists"
            :panel   :playlist
            :img-src "assets/piano.jpg"}]]
    [:> Grid {:xs   12
              :sm   6}
     [card {:label   "WWOZ Radio"
            :panel   :wwoz
            :img-src "assets/nola.jpg"}]]
    [:> Grid {:xs   12
              :sm   6}
     [card {:label   "Key, Tempo, Time Signature"
            :panel   :analysis
            :img-src "assets/sheet-music.jpg"}]]
    [:> Grid {:xs   12
              :sm   6}
     [card {:label   "Interval Timer" ;; "Power Hour"
            :panel   :powerhour
            :img-src "assets/hourglass.jpg"}]]
    [:> Grid {:xs   12
              :sm   6}
     [card {:label       "Search"
            :panel       :search
            #_#_:img-src "assets/hourglass.jpg"}]]]])

;; At one point I had accordions on the home page instead of fancy buttons
;; I liked the idea and maybe it is more functional. But it is prettier
;; using button links instead. Worth leaving this code around.
#_(comment
    (defn app-accordion [{:keys [app expanded? title subtitle link]}]
      [:> Accordion {:variant         "elevation"
                     :elevation       2
                     :expanded        @expanded?
                     :onChange        (fn [^js _e expanded]
                                        (reset! expanded? expanded))
                     :sx              {:height "100%"}
                     :TransitionProps {:timeout 0}}
       [:> AccordionSummary {:expandIcon (r/as-element [:> ExpandMoreIcon])
                             :sx         {:borderBottom "2px solid #EDEDED"}}
        [:> Stack {:spacing 1}
         [:> Stack {:direction  "row"
                    :spacing    1
                    :alignItems "flex-end"}
          [:> Typography {:variant "h4"} title]
          [link]]
         [:> Typography {:variant "h5" :color "text.secondary"} subtitle]]]
       [:> AccordionDetails {:sx {:marginTop "1.0rem"}}
        [app]]])

    (defn wwoz-links []
      [:> Stack {:direction "row" :spacing 1}
       [links/wwoz-to-spotify]
       [:> Link {:variant "body1"
                 :href    "https://github.com/benwiz/wwoz_to_spotify"
                 :target  "_blank"}
        "view on github"]])

    (def genre-playlists-expanded? (r/atom false))
    (def wwoz-expanded? (r/atom false))
    (def powerhour-expanded? (r/atom false))

    [:> Stack {:spacing 2
               :sx      {:paddingTop "3.0rem"
                         :maxWidth   "800px"
                         :alignSelf  "center"}}
     [app-accordion {:app       genre-playlists/app
                     :expanded? genre-playlists-expanded?
                     :title     "Genre Playlists"
                     :subtitle  "Add tracks to a playlist based on the genre"
                     :link      links/genre-playlists}]
     [app-accordion {:app       wwoz/embedded-player
                     :expanded? wwoz-expanded?
                     :title     "WWOZ on Spotify"
                     :subtitle  "Track WWOZ's 100 most recently played songs"
                     :link      wwoz-links}]
     [app-accordion {:app       powerhour/app
                     :expanded? powerhour-expanded?
                     :title     "Power Hour"
                     :subtitle  "Change songs every minute"
                     :link      links/powerhour}]]

    )
