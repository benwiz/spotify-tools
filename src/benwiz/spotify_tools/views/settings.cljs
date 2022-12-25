(ns benwiz.spotify-tools.views.settings
  (:require
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.subs :as subs]
   [re-com.core :as re-com :refer [at]]
   [re-frame.core :as rf]))

(defn title []
  [re-com/title
   :src   (at)
   :label "This is the Settings Page."
   :level :level1])

(defn debug-mode-checkbox []
  (let [debug? (rf/subscribe [::subs/db :debug?])]
    [re-com/checkbox
     :src   (at)
     :label "Debug Mode"
     :model @debug?
     :on-change #(rf/dispatch [::events/edit-db assoc :debug? %])]))

(defn panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[title]
              [re-com/line]
              [debug-mode-checkbox]]])
