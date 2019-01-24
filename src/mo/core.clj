(ns mo.core
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]))

(defonce mo (atom {::cache {}}))

(defn r! [] (reset! mo {::cache {}}))

(defn remove! [pattern])

(s/def ::id keyword?)
(s/def ::fn fn?)

(s/def ::mo (s/keys :req [::id ::fn]))
(defn reg-fn [m]
  {:pre [(s/valid? ::mo m)]}
  (let [meta (dissoc m ::fn)]
    (swap! mo update ::cache assoc meta (::fn m))))

(defn match [pattern]
  (->> (::cache @mo)
       keys
       (filter (fn [m] (every? (fn [[k v]] (= (k m) v)) pattern)))
       (map (fn [m] (assoc m ::fn (get-in @mo [::cache m]))))))

(defn matchf [pattern]
  (first (match pattern)))

(defn deep-merge [a b]
  (cond
    (and (map? a) (map? b))
    (merge-with deep-merge a b)
    (and (vector? a) (vector? b))
    (let [[c n] (if (> (count a) (count b)) [a (count b)] [b (count a)])]
      (vec (into (mapv deep-merge a b) (drop n c))))
    :else b))

(defn iter-apply [fns ctx*]
  (let [post? (:post-merge (meta ctx*))]
    (loop [[{id ::id f ::fn :as m} & oth] fns
           stack []
           ctx (if post? {} ctx*)]
      (if (nil? m)
        (cond-> (assoc ctx ::result (select-keys ctx stack))
          post? (deep-merge ctx*))
        (let [r (f ctx)]
          (cond
            (= ::nil r) (recur oth stack (dissoc ctx id))
            (nil? r) (recur oth (conj stack id) ctx)
            :else (recur oth (conj stack id) (deep-merge ctx {id r}))))))))

(defn dispatch-fn [_ arg]
  (cond
    (or (vector? arg) (seq? arg)) :collection
    :else :pattern))

(defmulti a #'dispatch-fn)

(defmethod a :pattern
  [pattern ctx*]
  (let [fns (match pattern)]
    (if (empty? fns) ctx* (iter-apply fns ctx*))))

(defmethod a :collection
  [ctx* fns] (iter-apply fns ctx*))

(comment
  (r!)

  )


