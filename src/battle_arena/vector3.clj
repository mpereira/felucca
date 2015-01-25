(ns src.battle-arena.vector3
  (:import [UnityEngine Vector3]))

(defn v*scalar ^Vector3 [^Vector3 v s] (Vector3/op_Multiply v (float s)))

(defn v*v ^Vector3 [^Vector3 v1 ^Vector3 v2] (Vector3/Scale v1 v2))

(defn v** ^Vector3 [a b]
  (if (instance? Vector3 a)
    (if (instance? Vector3 b) (v*v a b) (v*scalar a b))
    (if (instance? Vector3 b) (v*scalar b a) (* a b))))

(defn v* [& xs] (reduce v** xs))

(defn v- ^Vector3 [^Vector3 a ^Vector3 b] (Vector3/op_Subtraction a b))

(defn v+ ^Vector3 [^Vector3 a ^Vector3 b] (Vector3/op_Addition a b))

(defn v== [^Vector3 a ^Vector3 b] (Vector3/op_Equality a b))

(defn v!= [^Vector3 a ^Vector3 b] (Vector3/op_Inequality a b))

(defn vopposite ^Vector3 [^Vector3 a] (Vector3/op_UnaryNegation a))

(defn vdistance ^Vector3 [^Vector3 a ^Vector3 b] (.magnitude (v- a b)))

(defn v3 ^Vector3 [x y z] (Vector3. (float x) (float y) (float z)))
