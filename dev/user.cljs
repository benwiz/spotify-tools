(ns cljs.user
  "Commonly used symbols for easy access in the ClojureScript REPL during
  development."
  (:require
    [cljs.repl :refer (Error->map apropos dir doc error->str ex-str ex-triage
                       find-doc print-doc pst source)]
    [clojure.pprint :refer (pprint)]
    [clojure.string :as str]))

(comment

  (-> @re-frame.db/app-db
      :spotify/audio-features
      :items
      first
      second
      #_(->> (into []
                 (map (fn [[k v]]
                        [k (type v)])))
           (sort-by first)
           (into {}))
      )

  (->> {:valence          0.933,
        :loudness         -4.579,
        :key              4,
        :analysis_url
        "https://api.spotify.com/v1/audio-analysis/2hWI9GNr3kBrxZ7Mphho4Q",
        :duration_ms      337840,
        :instrumentalness 1.15E-5,
        :mode             0,
        :type             "audio_features",
        :energy           0.814,
        :speechiness      0.0381,
        :time_signature   4,
        :liveness         0.223,
        :id               "2hWI9GNr3kBrxZ7Mphho4Q",
        :danceability     0.803,
        :track_href       "https://api.spotify.com/v1/tracks/2hWI9GNr3kBrxZ7Mphho4Q",
        :uri              "spotify:track:2hWI9GNr3kBrxZ7Mphho4Q",
        :tempo            117.08,
        :acousticness     0.198}
       (sort-by first)
       sorted-map)
  {:analysis_url     "https://api.spotify.com/v1/audio-analysis/2hWI9GNr3kBrxZ7Mphho4Q"
   :id               "2hWI9GNr3kBrxZ7Mphho4Q"
   :track_href       "https://api.spotify.com/v1/tracks/2hWI9GNr3kBrxZ7Mphho4Q"
   :type             "audio_features"
   :uri              "spotify:track:2hWI9GNr3kBrxZ7Mphho4Q"
   :danceability     0.803
   :duration_ms      337840
   :energy           0.814
   :acousticness     0.198
   :instrumentalness 1.15E-5
   :key              4
   :liveness         0.223
   :loudness         -4.579
   :mode             0
   :speechiness      0.0381
   :tempo            117.08
   :time_signature   4
   :valence          0.933}

  )
