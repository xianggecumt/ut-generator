package com.github.xg.utgen.core.util;

/**
 * Created by yuxiangshi on 2017/8/28.
 */
public abstract class Tuple {

    public abstract <T> T first();

    public abstract <K> K second();

    public abstract <V> V third();

    public static <T, K> Tuple of(T t, K k) {
        return new Tuple2(t, k);
    }

    public static <T, K, V> Tuple of(T t, K k, V v) {
        return new Tuple3(t, k, v);
    }


    private static class Tuple2<T, K> extends Tuple {
        T t;
        K k;

        Tuple2(T t, K k) {
            this.t = t;
            this.k = k;
        }

        @Override
        public T first() {
            return t;
        }

        @Override
        public K second() {
            return k;
        }

        @Override
        public Object third() {
            throw new UnsupportedOperationException("Tuple2 don't contain third element!");
        }
    }

    private static class Tuple3<T, K, V> extends Tuple {
        T t;
        K k;
        V v;

        private Tuple3(T t, K k, V v) {
            this.t = t;
            this.k = k;
            this.v = v;
        }

        @Override
        public T first() {
            return t;
        }

        @Override
        public K second() {
            return k;
        }

        @Override
        public V third() {
            return v;
        }
    }

}


