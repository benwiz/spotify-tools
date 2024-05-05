(ns benwiz.spotify-tools.utils.spotify
  "Components and utility functions. Do not interface directly with Spotify
  in this namespace. Use benwiz.spotify-tools.utils.spotify-api instead."
  (:require ["@mui/icons-material/Equalizer" :default EqualizerIcon]
            ["@mui/joy/Autocomplete" :default Autocomplete]
            ["@mui/joy/AutocompleteOption" :default AutocompleteOption]
            ["@mui/joy/CircularProgress" :default CircularProgress]
            ["@mui/joy/ListItemContent" :default ListItemContent]
            ["@mui/joy/ListItemDecorator" :default ListItemDecorator]
            ["@mui/joy/Stack" :default Stack]
            ["@mui/joy/Typography" :default Typography]
            [benwiz.spotify-tools.config :as config]
            [benwiz.spotify-tools.events :as events]
            [benwiz.spotify-tools.subs :as subs]
            [benwiz.spotify-tools.utils.spotify-api :as spotify-api]
            [cljs-bean.core :refer [->clj]]
            [clojure.string :as str]
            [goog.object]
            [re-com.core :as re-com :refer [at]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; Authorization

(defn button []
  (if @(rf/subscribe [::subs/db :spotify/token])
    [re-com/label
     :src (at)
     :label "Successfully connected to Spotify"]
    [re-com/button
     :src (at)
     :label "Log in with Spotify"
     :on-click (fn []
                 (spotify-api/redirect! config/spotify-client-id))]))

;; Search components

(defn track-option
  [track] ;; {:keys [disc_number popularity duration_ms name explicit type external_urls external_ids preview_url track_number is_local id available_markets uri artists album href]}
  {:id      (:id track)
   :label   (:name track) ;; required
   :name    (:name track)
   :artists (into [] (map :name) (:artists track))
   :album   {:id    (:id (:album track))
             :name  (:name (:album track))
             :image (:url (first (:images (:album track))))}})

(defn track-options
  [tracks]
  (into [] (map track-option) tracks))

(defn extract-currently-playing
  [{:keys [is_playing item] :as _playback-state}]
  (when is_playing
    (track-option item)))

(defn autocomplete-option [props option]
  [:> AutocompleteOption (assoc props :key (:id option)) ;; TODO do a cool animated background that indicates currently playing
   [:> ListItemDecorator nil
    [:img {:src     (:image (:album option))
           :width   40
           :loading "lazy"}]]
   [:> ListItemContent {:sx {:marginLeft "1.0rem"}}
    [:> Stack {:direction "row"
               :spacing   1}
     (when (:currently-playing option)
       [:> ListItemDecorator {:sx {:marginLeft "1.0rem"}} [:> EqualizerIcon]])
     [:> Stack {:spacing 1}
      [:> Typography {:level "body1"} (:name option)]
      [:> Typography {:level "body2"} (str/join ", " (:artists option))]]]]])

(defn searcher []
  (let [token             (rf/subscribe [::subs/db :spotify/token])
        currently-playing (rf/subscribe [::subs/db :spotify/playback-state extract-currently-playing])
        options           (rf/subscribe [::subs/db :spotify/search-tracks track-options])
        loading?          (rf/subscribe [::subs/db :search some?])
        mobile?           (rf/subscribe [::subs/mobile?])]
    [:> Autocomplete
     {:sx                   {:minWidth "300px"
                             :fontSize (if @mobile?
                                         "1.5rem"
                                         "4.0rem")}
      ;; :variant              "soft"
      ;; :popupIcon            nil
      ;; TODO Prepend currently playing track to the top of the options always, but do it with a separate db subscription: spotify/current
      ;;       I'll need to ::events/get|read-current-track that updates the db at :spotify/current-tracks. Probably get it on a timer.
      :options              (if @currently-playing
                              (into [@currently-playing]
                                    (remove #(= (:id @currently-playing) (:id %)))
                                    @options)
                              @options)
      :loading              @loading?
      :placeholder          "Search for a track"
      :clearOnEscape        true ;; TOOD why isn't clear button showing also? it should by default
      :endDecorator         (when @loading?
                              (r/as-element
                                [:> CircularProgress
                                 {:size "sm"
                                  :sx   {:backgroundColor "background.surface"}}]))
      :onInputChange        (fn [^js _e value]
                              (when (not-empty value)
                                (rf/dispatch (into [::events/debounce]
                                                   (spotify-api/search @token value)))))
      :onChange             (fn [^js _e value]
                              (if-some [value (->clj value)]
                                (do
                                  (rf/dispatch [::events/edit-db assoc :analysis/selection value])
                                  (rf/dispatch (spotify-api/audio-features @token (:id value))))
                                (do
                                  (rf/dispatch [::events/edit-db dissoc :analysis/selection])
                                  (rf/dispatch [::events/edit-db dissoc :analysis/audio-features]))))
      :isOptionEqualToValue (fn [option value]
                              (= (goog.object/get option "id")
                                 (goog.object/get value "id")))
      :renderOption         (fn [props option]
                              (let [props  (->clj props)
                                    option (->clj option)]
                                (r/as-element
                                  ^{:key (:id option)}
                                  [autocomplete-option
                                   props
                                   (assoc option :currently-playing (= (:id @currently-playing) (:id option)))])))}]))
