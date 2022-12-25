(ns benwiz.spotify-tools.subs
  (:require [re-frame.core :as rf]))

;; idk if using a generic subscription is good or bad.
;; I assume as long as I pass in deffed fns instead of
;; anonymous functions I should gain any re-use benefits
;; of dedicated subscriptions. However, if it is caching
;; based on the query-id then this is a horrible idea.
(rf/reg-sub
  ::db
  (fn [db [_ k f]]
    (cond-> db
      k (as-> db
            (if (vector? k)
              (get-in db k)
              (get db k)))
      f f)))

(rf/reg-sub
  ::playlist-search-term
  (fn [db [_]]
    (:playlist-search-term db)))

(rf/reg-sub
  ::mobile?
  (fn [db [_]]
    (= :mobile (:window/breakpoint db))))
