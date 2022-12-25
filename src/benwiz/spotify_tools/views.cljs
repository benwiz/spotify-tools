(ns benwiz.spotify-tools.views
  (:require
   ["@mui/joy/Stack" :default Stack]
   ["@mui/joy/styles" :refer [CssVarsProvider extendTheme]]
   ["@mui/material/useMediaQuery" :default useMediaQuery]
   [benwiz.spotify-tools.routes :as routes]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.views.about :as about]
   [benwiz.spotify-tools.views.analysis :as analysis]
   [benwiz.spotify-tools.views.genre-playlists :as genre-playlists]
   [benwiz.spotify-tools.views.home :as home]
   [benwiz.spotify-tools.views.powerhour :as powerhour]
   [benwiz.spotify-tools.views.settings :as settings]
   [benwiz.spotify-tools.views.spotifylogin :as spotifylogin]
   [benwiz.spotify-tools.views.wwoz-to-spotify :as wwoz-to-spotify]
   [cljs-bean.core :refer [->js]]
   [re-com.core :as re-com]
   [re-frame.core :as rf]))

(defmethod routes/panels :spotifylogin [] [spotifylogin/panel])

(defmethod routes/panels :home [] [home/panel])

(defmethod routes/panels :about [] [about/panel])

(defmethod routes/panels :settings [] [settings/panel])

(defmethod routes/panels :playlist [] [genre-playlists/panel])

(defmethod routes/panels :analysis [] [analysis/panel])

(defmethod routes/panels :wwoz [] [wwoz-to-spotify/panel])

(defmethod routes/panels :powerhour [] [powerhour/panel])

;; (def spanish-tiles-orange-blue "https://media.istockphoto.com/id/1292585905/vector/oriental-moroccan-tile-seamless-pattern.jpg?s=612x612&w=0&k=20&c=7VzHxcN1yE2Spz1SxJAXlTzGqXK2wEWgpmzKPDcE8Bk=")
;; (def spanish-tiles-white-blue "https://media.istockphoto.com/id/1174584974/vector/decorative-seamless-tile-pattern.jpg?s=612x612&w=0&k=20&c=4BxjzozC5fOxZ4_BycqMTaZX1HIBP9lilqoYGMLF8Jw=")
;; (def succulent "https://images.unsplash.com/photo-1494185728463-86366f396213?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1470&q=80")
;; (def plant "https://images.unsplash.com/photo-1515595967223-f9fa59af5a3b?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=687&q=80")
;; (def sand "https://images.unsplash.com/photo-1482977036925-e8fcaa643657?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1974&q=80")
(def sand-plant "https://images.unsplash.com/photo-1493382051629-7eb03ec93ea2?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=1470&q=80")

(def spotify-green "#1DB954")

(def theme
  (extendTheme
    (->js
      {:colorSchemes
       {:light
        {:palette {:success {:plainColor      spotify-green
                             ;; only plain seems to look good with spotify-green
                             #_#_:softColor   spotify-green
                             #_#_:softHoverBg spotify-green}}}}
       :components
       {:JoyStack            {:defaultProps {:spacing 2}}
        :JoyChip             {:defaultProps {:color   "success"
                                             :size    "lg"
                                             :variant "soft"}}
        :JoyButton           {:defaultProps {:color   "success"
                                             :variant "soft"}}
        #_#_:JoyIconButton   {:styleOverrides {:root {:color spotify-green}}}
        :JoyAutocomplete     {:defaultProps   {:color   "success"
                                               :variant "outlined"}
                              #_#_:styleOverrides {:root
                                               {:borderColor "var(--joy-palette-neutral-200)"
                                                :color       "var(--joy-palette-neutral-800)"}}}
        :JoyLinearProgress   {:defaultProps   {:color "success"}
                              :styleOverrides {:root {:color spotify-green}}}
        :JoyCircularProgress {:defaultProps   {:variant "plain"}
                              :styleOverrides {:root {:color spotify-green}}}}})))

(defn main-panel []
  (let [active-panel (rf/subscribe [::subs/db :active-panel])]
    [:> CssVarsProvider {:theme theme
                         ;; :defaultMode "dark"
                         ;; :modeStorageKey       "spotify-tools_joy-system-mode"
                         ;; :disableNestedContext true
                         }
     #_#_:div {:style { ;; :height               "100%" ;; good for long page, bad for short page
                       :backgroundImage      (str "url(\"" sand-plant "\")")
                       :backgroundRepeat     "no-repeat"
                       :backgroundAttachment "fixed"}}
     [:> Stack {:spacing 2
                :sx      {:height         "100%"
                          :width          "100%"
                          :padding        "1.2rem"
                          #_#_:backdropFilter "blur(5px)"
                          #_#_:background     "rgba(255, 255, 255, 0.5)"}}
      (routes/panels @active-panel)]]))

(comment
  (js/Math.ceil (/ (-> @re-frame.db/app-db :spotify/user-tracks :items count) 1000))

  (def s (pr-str (-> @re-frame.db/app-db :spotify/user-tracks :items)))
  (type s)
  (count s)
  (.. (js/Blob. [s]) -size)

  )
