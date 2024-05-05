(ns benwiz.spotify-tools.core
  (:require
   [benwiz.spotify-tools.config :as config]
   [benwiz.spotify-tools.events :as events]
   [benwiz.spotify-tools.routes :as routes]
   [benwiz.spotify-tools.subs :as subs]
   [benwiz.spotify-tools.utils.spotify-api :as spotify-api]
   [benwiz.spotify-tools.views :as views]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]
   [vimsical.re-frame.fx.track :as track]))

(defn data-pull-init [] ;; there is probably a better spot to initialize this stuff
  (rf/dispatch [::events/interval
                {:id        :timer
                 :action    :start
                 :frequency 1000
                 :events    [[::events/edit-db update :spotify/token spotify-api/valid-token!]]}])
  (rf/dispatch [::events/register-track
                  {:id           :active-panel-init
                   :subscription [::subs/db :active-panel]
                   :event-fn     (fn [active-panel]
                                   [::events/init-panel active-panel])}]))

(defn clear-interval-cache! []
  (events/interval-handler {:action :clean}))

(defn clear-track-cache! []
  (into []
        (comp
          (map first)
          (map #(rf/dispatch [::events/dispose-track %])))
        @track/register))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (clear-interval-cache!)
  (clear-track-cache!)
  (data-pull-init)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (rf/dispatch-sync [::events/initialize-db])
  (rf/dispatch-sync [::events/window-resized (.. js/window -innerWidth)])
  (rf/dispatch-sync [::events/init-window-events])
  (dev-setup)
  (mount-root))
