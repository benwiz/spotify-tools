(ns benwiz.spotify-tools.db
  (:require [benwiz.spotify-tools.config :as config]
            [clojure.edn :as edn]))

;; TODO use malli

;; TODO move some the non hardcoded stuff to the init event, leave behind nil values until I get malli up
;; there are also additions for each app not currently documented here.
(def default-db
  {:name                   "Spotify Tools"
   :debug?                 config/debug?
   :spotify/token          nil
   :spotify/user-artsits   nil
   :spotify/user-tracks    nil
   :spotify/user-playlists nil
   :spotify/devices        nil
   :spotify/playback-state nil
   :powerhour/interval     60  ;; seconds
   :powerhour/duration     60  ;; minutes
   :powerhour/timer        nil ;; seconds
   :powerhour/playing      false})
