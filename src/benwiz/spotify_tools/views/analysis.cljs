(ns benwiz.spotify-tools.views.analysis
  (:require
   ["@mui/icons-material/Clear" :default ClearIcon]
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
   ["@mui/joy/Sheet" :default Sheet]
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/Typography" :default Typography]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.components :as c]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [cljs-bean.core :refer [->clj]]
   [clojure.string :as str]
   [goog.object]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(defn track-options
  [tracks]
  (into []
        (map (fn [track] ;; {:keys [disc_number popularity duration_ms name explicit type external_urls external_ids preview_url track_number is_local id available_markets uri artists album href]}
               {:id      (:id track)
                :label   (:name track) ;; required
                :name    (:name track)
                :artists (into [] (map :name) (:artists track))
                :album   {:id    (:id (:album track))
                          :name  (:name (:album track))
                          :image (:url (first (:images (:album track))))}}))
        tracks))

(defn autocomplete-option [props option]
  [:> AutocompleteOption (assoc props :key (:id option))
   [:> ListItemDecorator nil
    [:img {:src     (:image (:album option))
           :width   40
           :loading "lazy"}]]
   [:> ListItemContent {:sx {:marginLeft "1.0rem"}}
    [:> Typography {:level "body1"}
     (:name option)]
    [:> Typography {:level "body2"}
     (str/join ", " (:artists option))]]])

(defn searcher []
  (let [token     (rf/subscribe [::subs/db :spotify/token])
        options   (rf/subscribe [::subs/db :spotify/search-tracks track-options])
        loading?  (rf/subscribe [::subs/db :search some?])
        mobile?   (rf/subscribe [::subs/mobile?])]
    [:> Autocomplete {:sx                   {:minWidth "300px"
                                             :fontSize (if @mobile?
                                                         "1.5rem"
                                                         "4.0rem")}
                      ;; :variant              "soft"
                      ;; :popupIcon            nil
                      :options              @options
                      :loading              @loading?
                      :placeholder          "Search for a track"
                      :clearOnEscape        true
                      :endDecorator         (when @loading?
                                              (r/as-element
                                                [:> CircularProgress
                                                 {:size "sm"
                                                  :sx   {:backgroundColor "background.surface"}}]))
                      :onInputChange        (fn [^js _e value]
                                              (when (not-empty value)
                                                (rf/dispatch (into [::events/debounce]
                                                                   (spotify/search @token value)))))
                      :onChange             (fn [^js _e value]
                                              (if-some [value (->clj value)]
                                                (do
                                                  (rf/dispatch [::events/edit-db assoc :analysis/selection value])
                                                  (rf/dispatch (spotify/audio-features @token (:id value))))
                                                (do
                                                  (rf/dispatch [::events/edit-db dissoc :analysis/selection])
                                                  (rf/dispatch [::events/edit-db dissoc :analysis/audio-features]))))
                      :isOptionEqualToValue (fn [option value]
                                              (= (goog.object/get option "id")
                                                 (goog.object/get value "id")))
                      :renderOption         (fn [props option]
                                              (let [props  (->clj props)
                                                    option (->clj option)]
                                                [autocomplete-option props option]
                                                (r/as-element
                                                  ^{:key (:id option)}
                                                  [autocomplete-option props option])))}]))

(defn selected-track []
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
       (str/join ", " (:artists @selection))]]
     [:> IconButton {:variant "plain"
                     :color   "neutral"
                     :onClick #(do
                                 (rf/dispatch [::events/edit-db dissoc :analysis/selection])
                                 (rf/dispatch [::events/edit-db dissoc :analysis/audio-features]))}
      [:> ClearIcon {:sx {:fontSize "1.0rem"}}]]]))

(defn key-signature []
  (let [audio-features (rf/subscribe [::subs/db :analysis/audio-features])]
    (when @audio-features
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
         [selected-track]
         [searcher])
       [key-signature]]
      [spotify/button])))

(defn panel []
  (let [token (rf/subscribe [::subs/db :spotify/token])]
    [:> Stack {:spacing    2
               :alignItems "stretch"}
     [c/header {:title "Analysis"}]
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
