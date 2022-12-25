(ns benwiz.spotify-tools.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [benwiz.spotify-tools.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
