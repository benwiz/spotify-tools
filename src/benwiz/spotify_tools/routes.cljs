(ns benwiz.spotify-tools.routes
  (:require
   [benwiz.spotify-tools.events :as events]
   [bidi.bidi :as bidi]
   [clojure.string :as str]
   [pushy.core :as pushy]
   [re-frame.core :as rf]))

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])

(def routes
  (atom
    ["/" {""             :home
          "about"        :about
          "settings"     :settings
          "spotifylogin" :spotifylogin
          "playlist"     :playlist
          "analysis"     :analysis
          "wwoz"         :wwoz
          "timer"        :powerhour}]))

(defn parse ;; using hash routing because it simplifies using "/" or "/spotify/" ;; TODO use normal routes
  [url]
  (let [path (or (second (str/split url #"#" 2)) "/")]
    (if (str/starts-with? (str path) "access_token")
      {:handler :spotifylogin} ;; TODO this is janky af
      (bidi/match-route @routes path))))

(defn url-for
  [& args]
  (str "#" (apply bidi/path-for (into [@routes] args))))

(defn dispatch
  [route]
  (rf/dispatch [::events/set-active-panel (:handler route)]))

(defonce history
  (pushy/pushy dispatch parse))

(defn navigate!
  [handler]
  (pushy/set-token! history (url-for handler)))

(defn start!
  []
  (pushy/start! history))

(rf/reg-fx
  :navigate
  (fn [handler]
    (navigate! handler)))
