(ns mo.internals-test
  (:require
   [mo.core :as mo]
   [clojure.test :refer :all]))

(deftest test-deep-merge
  (is (= (mo/deep-merge [] [1]) [1]))

  (is (= (mo/deep-merge [1] []) [1]))

  (is (= (mo/deep-merge [1 2] [3 4]) [3 4]))

  (is (= (mo/deep-merge [1 2] [3 4 5]) [3 4 5]))

  (is (= (mo/deep-merge [1 2 3] [4 5]) [4 5 3])))
