package foundation.input;

import foundation.tick.RegisteredTickable;
import foundation.tick.TickOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InputHandler implements RegisteredTickable {
    ArrayList<InputData<?>> queuedInputs = new ArrayList<>();

    //We store each input event in a TreeMap, sorted by the event order
    //This means that we can execute them in order, but also that each event
    //order can only have one event per input type. This is because each event type
    //has its own separate TreeMap, so for two events with the same order but different
    //input types, they won't be stored in the same TreeMap.
    HashMap<InputType<?>, TreeMap<InputEvent, HashSet<InputListenerEventData<?>>>> eventListeners = new HashMap<>();

    public InputHandler() {
        for (InputType<?> type : InputType.values()) {
            eventListeners.put(type, new TreeMap<>());
        }
        registerTickable();
    }

    synchronized public <T extends java.awt.event.InputEvent> void addInput(InputType<T> type, Consumer<T> event, Predicate<T> condition, InputEvent order, boolean blocking) {
        if (!eventListeners.get(type).containsKey(order))
            eventListeners.get(type).put(order, new HashSet<>());
        eventListeners.get(type).get(order).add(new InputListenerEventData<>(event, condition, blocking));
    }

    synchronized public <T extends java.awt.event.InputEvent> void queueInput(InputType<T> type, T event) {
        queuedInputs.add(new InputData<>(type, event));
    }

    @Override
    synchronized public void tick(float deltaTime) {
        for (InputData<? extends java.awt.event.InputEvent> input : queuedInputs) {
            processInput(input);
        }
        queuedInputs.clear();
    }

    private <T extends java.awt.event.InputEvent> void processInput(InputData<T> inputData) {
        for (HashSet<InputListenerEventData<?>> set : eventListeners.get(inputData.type).values()) {
            for (InputListenerEventData<? extends java.awt.event.InputEvent> listenerEventData : set) {
                //Safe cast as we know that all values inserted into the TreeMap must be of type
                //InputListenerEventData<T> because of how they're added in the addInput method.
                //The unchecked cast is there to hide the fact that we're storing multiple TreeMaps
                //with different type parameters in the same HashMap
                InputListenerEventData<T> castListenerEventData = (InputListenerEventData<T>) listenerEventData;
                if (castListenerEventData.condition.test(inputData.event)) {
                    castListenerEventData.event.accept(inputData.event);
                    if (castListenerEventData.blocking)
                        break;
                }
            }
        }
    }

    @Override
    public TickOrder getTickOrder() {
        return TickOrder.PLAYER_INPUT;
    }

    @Override
    public void delete() {
        removeTickable();
    }

    //The data used to store queued up inputs
    private static class InputData<T extends java.awt.event.InputEvent> {
        public InputType<T> type;
        public T event;

        public InputData(InputType<T> type, T event) {
            this.type = type;
            this.event = event;
        }
    }

    //The data used to store the Consumers we run when an input is received
    private static class InputListenerEventData<T extends java.awt.event.InputEvent> {
        public Consumer<T> event;
        public Predicate<T> condition;
        public boolean blocking; //Do we block the input from reaching events lower on the input order? Only applies if condition is true

        public InputListenerEventData(Consumer<T> event, Predicate<T> condition, boolean blocking) {
            this.event = event;
            this.blocking = blocking;
            this.condition = condition;
        }
    }
}
