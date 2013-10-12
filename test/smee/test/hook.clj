(ns smee.test.hook
  (:require [clojure.test :refer :all]
            [smee.hook :refer :all]))

(use-fixtures :each
  (fn [f]
    (remove-all-hooks)
    (start-processing-hooks)
    (f)
    (stop-processing-hooks)))

(deftest test-remove-all-hooks
  (is (empty? (registered-hooks)))
  (add-hook :some-hook (fn [& _]))
  (is (not (empty? (registered-hooks))))
  (remove-all-hooks)
  (is (empty? (registered-hooks))))

(deftest test-remove-hook
  (let [c (atom 0)
        p (promise)
        hook-fn (fn [& _]
                  (swap! c inc)
                  (deliver @p 1))]
    (is (empty? (:hook (registered-hooks))))
    (add-hook :hook hook-fn)
    (is (not (empty? (:hook (registered-hooks)))))
    (remove-hook :hook hook-fn)
    (is (empty? (:hook (registered-hooks))))
    (run-hook :hook)
    (is (true? (deref p 250 true))
        "This should timeout. Tick tick tick tick.")))

(deftest test-run-hook
  (let [c (atom 0)
        p (atom (promise))]
    (add-hook :some-hook
              (fn [& _]
                (swap! c inc)
                (deliver @p 1)))
    (testing "happy path"
      (is (zero? @c))
      (run-hook :some-hook)
      (is (deref @p 1000 false) "deref took too long")
      (is (= 1 @c)))
    (testing "missing hook"
      (run-hook :nonexistant-hook)
      ;; nothing should happen, sanity check
      (is (= 1 @c)))
    (testing "exceptional hooks"
      (reset! p (promise))
      (add-hook :exceptional-hook
                (fn []
                  (throw (Exception. "unexpected test exception"))))
      (try
        (run-hook :exceptional-hook)
        (catch Exception e
          (is false (.getMessage e))))
      (run-hook :some-hook)
      (is (deref @p 1000 false) "deref took too long")
      (is (= 2 @c)))))

(deftest test-hook-threads
  ;; hooks run in a separate thread.  Sanity check this.
  (let [thread-name (.getId (Thread/currentThread))
        hook-thread-name (atom "")
        p (promise)]
    (add-hook :some-hook
              (fn [& _]
                (reset! hook-thread-name
                        (.getId (Thread/currentThread)))
                (deliver p true)))
    (run-hook :some-hook)
    (is (deref p 1000 false) "deref took too long")
    (is (not (= thread-name @hook-thread-name)))))

(deftest test-hook-args
  (let [args (atom [])
        p (atom (promise))]
    (add-hook :some-hook
              (fn [& a]
                (swap! args conj a)
                (deliver @p 1)))
    (is (empty? @args))
    (run-hook :some-hook 1 2 3)
    (is (deref @p 1000 false) "deref took too long")
    (is (= [[1 2 3]] @args))
    (reset! p (promise))
    (run-hook :some-hook)
    (is (deref @p 1000 false) "deref took too long")
    (is (= [[1 2 3] nil] @args))))

(deftest test-multiple-callbacks
  (let [results (atom [])
        p (promise)
        q (promise)
        cb-1 (fn [& a]
               (swap! results conj "hook 1")
               (deliver p 1))]
    (add-hook :some-hook cb-1)
    ;; add it again to make sure it's deduped
    (add-hook :some-hook cb-1)
    (add-hook :some-hook
              (fn [& a]
                (swap! results conj "hook 2")
                (deliver q 1)))
    (is (empty? @results))
    (run-hook :some-hook)
    (is (deref p 1000 false) "deref took too long")
    (is (deref q 1000 false) "deref took too long")
    (is (= #{"hook 1" "hook 2"} (set @results)))))

(deftest test-long-running-callback
  (let [results (atom [])
        p (promise)
        q (promise)]
    (add-hook :fast-hook
              (fn [& a]
                (swap! results conj {:name "speedy gonzales"
                                     :time (System/currentTimeMillis)})
                (deliver q 1)))
    (add-hook :slow-hook
              (fn [& a]
                (Thread/sleep 500)
                (swap! results conj {:name "regular gonzales"
                                     :time (System/currentTimeMillis)})
                (deliver p 1)))
    (is (empty? @results))
    (run-hook :slow-hook)
    (run-hook :fast-hook)
    (is (deref p 1000 false) "deref took too long")
    (is (deref q 1000 false) "deref took too long")
    (let [[{speedy-name :name speedy-time :time}
           {slow-name :name slow-time :time}] @results]
      (is (= speedy-name "speedy gonzales"))
      (is (= slow-name "regular gonzales"))
      (is (> slow-time speedy-time)))))
