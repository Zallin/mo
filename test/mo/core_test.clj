(ns mo.core-test
  (:require
   [mo.core :as mo]
   [matcho.core :as matcho]
   [clojure.test :refer :all]))

(deftest basics
  ;; let us reg a function
  (mo/reg-fn
   {::mo/id :greet
    ::mo/fn
    (fn [{u :user}] (str "Hello, " (:name u) "!"))})

  ;; apply pattern |             to context |
  ;;               V                        V
  (def res (mo/a {::mo/id :greet} {:user {:name "Tim" :role "admin"}}))

  (matcho/assert
   {:user {:name "Tim" :role "admin"}
    ;; the context is enriched with the result of pattern application
    :greet "Hello, Tim!"}
   res)

  ;; result is also stored separately from the context
  (is (= (::mo/result res) {:greet "Hello, Tim!"})))

(deftest when-multiple-functions-match

  ;; mo is about applying multiple functions
  ;; to one shared context

  (mo/reg-fn
   {::mo/id :english
    :language-group :romance
    ::mo/fn
    (fn [{n :name}] (str "Hello, " n))})

  (mo/reg-fn
   {::mo/id :spanish
    :language-group :romance
    ::mo/fn
    (fn [{n :name}] (str "Hola, " n))})

  (mo/reg-fn
   {::mo/id :russian
    :language-group :slavic
    ::mo/fn
    (fn [{n :name}] (str "Привет, " n))})

  (matcho/assert
   {:english "Hello, Volodya"
    :spanish "Hola, Volodya"}
   ;; pattern can match to many functions
   (mo/a {:language-group :romance} {:name "Volodya"})))

(deftest how-merge-works
  (mo/reg-fn
   {::mo/id :user
    ::mo/fn
    (fn [_] {:name "Jack" :family "Jones" :roles ["admin" "owner"]})})

  ;; results of application are deep merged into context
  (matcho/assert
   ;;                                        vector merge is positional |
   ;;                                                                   V
   {:user {:name "Jack" :middle "M." :family "Jones" :roles ["admin" "owner" "user"]}}
   (mo/a {::mo/id :user}
         {:user
          {:name "Bob"
           :middle "M."
           :family "Smiths"
           :roles ["just" "a" "user"]}})))

(deftest mo-nil
  ;; if you apply same function multiple times
  ;; you may want to set result of previous application
  ;; to nil under some conditions

  (mo/reg-fn
   {::mo/id :user
    ::mo/fn
    ;; this is achieved by using |
    ;;                           V
    (fn [{u :user}] (if u ::mo/nil {:name "Tim"}))})

  (def ctx (mo/a {::mo/id :user} {}))

  ;; the first application
  (matcho/assert {:user {:name "Tim"}} ctx)

  ;;the second one
  (matcho/assert {:user nil?} (mo/a {::mo/id :user} ctx)))

(deftest pure-match
  ;; you can also match functions
  ;; and apply them yourself

  (mo/reg-fn
   {::mo/id :plus
    ::mo/fn +})

  (is (= (apply (mo/matchf {::mo/id :plus}) [1 2]) 2)))

(deftest test-collection-apply
  ;; apply also works on collection of mo fns

  ;; it is used in case you want to do some
  ;; calculations on function metadata
  ;; and decide what to apply based on that

  ;; we reg three fns
  (mo/reg-fn
   {::mo/id :square
    :group :api-operations
    ::mo/fn (fn [{i :input}] (* i i))})

  (mo/reg-fn
   {::mo/id :print
    :group :api-operations
    ::mo/fn (fn [{i :input}] (println "Input is " i) 'done)})

  (mo/reg-fn
   {::mo/id :exception
    :group :api-operations
    ::mo/fn (fn [_] (Exception. "Things crashed!"))})

  (def call-chain {:apply #{:square :print}})

  ;; do filtering based on superimposed criteria
  ;; and apply
  (matcho/match
   (->> (mo/match {:group :api-operations})
        (filter (fn [op] (contains? (:apply call-chain) (::mo/id op))))
        (mo/a {:input 5}))
   {:square 25
    :print 'done}))

(deftest test-deep-merge

  (is (= (mo/deep-merge [] [1]) [1]))

  (is (= (mo/deep-merge [1] []) [1]))

  (is (= (mo/deep-merge [1 2] [3 4]) [3 4]))

  (is (= (mo/deep-merge [1 2] [3 4 5]) [3 4 5]))

  (is (= (mo/deep-merge [1 2 3] [4 5]) [4 5 3])))
