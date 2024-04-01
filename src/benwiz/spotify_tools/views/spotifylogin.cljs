(ns benwiz.spotify-tools.views.spotifylogin
  (:require
   ["@mui/joy/Chip" :default Chip]
   [benwiz.spotify-tools.config :as config]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.routes :as routes]
   [benwiz.spotify-tools.utils.spotify :as spotify]
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as rf]))

(defn panel []
  (let [access-token (-> (subs js/window.location.hash 1)
                         js/URLSearchParams.
                         (.get "access_token"))
        expires-in   (some-> (subs js/window.location.hash 1)
                             js/URLSearchParams.
                             (.get "expires_in")
                             not-empty
                             js/parseInt)
        path         (-> (subs js/window.location.hash 1)
                         js/URLSearchParams.
                         (.get "state"))]
    ;; (prn 'path access-token expires-in path)
    ;; TODO I'm 100% sure I should not be using the component render to dispatch an event
    ;; there must be some sort of way in hooking into page load or something.
    ;; page could load from direct url not following a :navigate event so I'm unsure what to do.
    ;; I probably can run a redirect 301 (or whatever the code is) and run dispatch during that.
    (rf/dispatch [::events/set-spotify-token
                  {:access-token access-token
                   :expires-at   (js/Date. (+ (.getTime (js/Date.))
                                              (* expires-in 1000)))}
                  (:handler (routes/parse path))])
    [re-com/label
     :src   (at)
     :label "Loading..."]))

(defn login-button []
  [:> Chip {:color   "neutral"
            :onClick (fn [_]
                       (spotify/redirect! config/spotify-client-id))}
   "Log in with Spotify"])

(defn logout-button []
  [:> Chip {:color   "neutral"
            :onClick (fn []
                       (rf/dispatch [::events/delete-spotify-token]))}
   "Disconnect from Spotify"])
