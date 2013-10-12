(ns smee.hook
  (:require [clojure.core.async :refer [<! >! chan close! go put! >!! pipe]]
            [clojure.tools.logging :as log]))

(def ^:private hook-chan
  "The channel that holds all of the pending hooks to call."
  (atom (chan)))

(def ^:private hooks
  "A map of hook -> set of callback functions."
  (atom {}))

(def ^:private running
  "An atom that holds the running state for the hook processing loop."
  (atom false))

(defn registered-hooks
  "Return the map of registered hooks."
  []
  @hooks)

(defn add-hook
  "Register a new callback-fn for hook"
  [hook callback-fn]
  (if (get @hooks hook)
    (swap! hooks update-in [hook] conj callback-fn)
    (swap! hooks assoc hook #{callback-fn})))

(defn remove-hook
  "Unregister the callback-fn for the hook"
  [hook callback-fn]
  (swap! hooks update-in [hook] clojure.set/difference #{callback-fn}))

(defn remove-all-hooks
  "Remove all registered callbacks for hook, or all hooks if no hook
  is passed."
  ([]
     (reset! hooks {}))
  ([hook]
     (swap! hooks dissoc hook)))

(defn run-hook
  "Asynchronously call all of the registered callbacks for the hook,
  passing those functions all of args."
  [hook & args]
  (let [result-channel (chan)]
    (put! @hook-chan {:hook hook
                      :args args
                      :result-channel result-channel})
    result-channel))

(defn run-callbacks
  "Sequentially applies callbacks to args, returning a closed channel
  with the results.  If one of the callback functions results in an
  exception, it will be logged, but otherwise ignored.  Exception
  handling should be done inside the callback."
  [result-channel callbacks args]
  (let [from-channel (chan (count callbacks))]
    (doseq [cb callbacks]
      (try
        (>!! from-channel (apply cb args))
        (catch Exception e
          ;; exceptions in hooks musn't cause
          ;; the loop to die
          (log/error e "Error while running hook callback"))))
    (doto from-channel
      close!)))

(defn process-one-hook
  "Given a hook and arguments, asynchronously call all of the
  registered callbacks defined for the hook, piping the result into
  the result-channel."
  [hook args result-channel]
  (when @running
    (if-let [callback-fns (seq (get @hooks hook))]
      (pipe (run-callbacks result-channel callback-fns args)
            result-channel)
      (log/info "No callback functions defined for hook:" hook))))

(defn start-processing-hooks
  "Start the hook processing loop."
  []
  (locking running
    (when-not @running
      (reset! running true)
      (go (while @running
            (let [{:keys [hook args result-channel]} (<! @hook-chan)]
              (process-one-hook hook args result-channel)))))))

(defn stop-processing-hooks
  "'Stop me, Smee!'

  Stop the hook processing loop and reset the hook channel"
  []
  (locking running
    (dosync
     (when @running
       (reset! running false)
       (close! @hook-chan)
       (reset! hook-chan (chan))))))
