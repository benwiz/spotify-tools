(ns benwiz.spotify-tools.views.analysis
  (:require
   ["@mui/icons-material/Clear" :default ClearIcon]
   ["@mui/joy/AspectRatio" :default AspectRatio]
   ["@mui/joy/Card" :default Card]
   ["@mui/joy/CardContent" :default CardContent]
   ["@mui/joy/CardOverflow" :default CardOverflow]
   ["@mui/joy/IconButton" :default IconButton]
   ["@mui/joy/Sheet" :default Sheet]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.links :as links]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [clojure.string :as str]
   [goog.object]
   [re-frame.core :as rf]))

;; TODO search by instrument (or any facet)

(defn selected-track [] ;; TODO extract to a generalized namespace, it'll be useful elsewhere
  (let [selection (rf/subscribe [::subs/db :analysis/selection])
        mobile?   (rf/subscribe [::subs/mobile?])]
    [:> Card {:orientation "horizontal"
              :variant     "outlined"}
     [:> CardOverflow nil
      [:> AspectRatio {:ratio 1
                       :sx    {:width (if @mobile? "80px" "150px")}}
       [:img {:src     (:image (:album @selection))
              :loading "lazy"}]]]
     [:> CardContent {:sx {:px             2
                           :justifyContent "center"}}
      [:> Typography {:level "body1"}
       (:name @selection)]
      [:> Typography {:level "body2"}
       (str/join ", " (:artists @selection))]
      [links/spotify-track (:id @selection)]]
     [:> IconButton {:variant "plain"
                     :color   "neutral"
                     :onClick #(do
                                 (rf/dispatch [::events/edit-db dissoc :analysis/selection])
                                 (rf/dispatch [::events/edit-db dissoc :analysis/audio-features]))}
      [:> ClearIcon {:sx {:fontSize "1.0rem"}}]]]))

(defn key-signature []
  (let [audio-features (rf/subscribe [::subs/db :analysis/audio-features])]
    (when @audio-features ;; TODO don't do when at the top, handle the specific cases
      [:> Stack {:spacing    1
                 :alignItems "center"}
       [:> Typography {:fontSize   "10.0rem"
                       :textColor  "success.600"
                       :lineHeight "sm"}
        (str (nth ["C" "C#/Db" "D" "D#/Eb" "E" "F"
                   "F#/Gb" "G" "G#/Ab" "A" "A#/Bb" "B"]
                  (:key @audio-features))
             (when (zero? (:mode @audio-features))
               "m"))]
       [:> Typography {:fontSize  "3.0rem"
                       :textColor "success.400"}
        (str (:time_signature @audio-features) "/" 4
             " at " (:tempo @audio-features) " bpm")]])))

(defn app [] ;; note that the entire app lives inside the card, no cards inside the app
  (let [token     (rf/subscribe [::subs/db :spotify/token])
        selection (rf/subscribe [::subs/db :analysis/selection])]
    (if @token
      [:> Stack {:spacing    3
                 :alignItems "center"}
       (if @selection
         [:<>
          [selected-track]
          [key-signature]]
         [spotify/searcher])]
      [spotify/button])))

(defn panel []
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [c/header {:title "Key, Tempo, and Time Signature"}]
     (if @token
       #_[:> Card {:variant "outlined"
                 :sx      {:alignSelf "center"
                           :maxWidth  "800px"
                           :width     "100%"}}
        [:> CardContent nil
         [app]]]
       [:> Sheet {:sx {:alignSelf "center"
                       :maxWidth  "800px"
                       :width     "100%"}}
        [app]]
       [spotify/button])]))
