package me.glicz.binarytagops;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.kyori.adventure.nbt.*;

import java.util.Optional;

import static net.kyori.adventure.nbt.ByteArrayBinaryTag.byteArrayBinaryTag;
import static net.kyori.adventure.nbt.ByteBinaryTag.byteBinaryTag;
import static net.kyori.adventure.nbt.IntArrayBinaryTag.intArrayBinaryTag;
import static net.kyori.adventure.nbt.IntBinaryTag.intBinaryTag;
import static net.kyori.adventure.nbt.LongArrayBinaryTag.longArrayBinaryTag;
import static net.kyori.adventure.nbt.LongBinaryTag.longBinaryTag;

@SuppressWarnings("ClassEscapesDefinedScope")
sealed interface ListCollector {
    static Optional<ListCollector> listCollector(BinaryTag input) {
        return Optional.ofNullable(switch (input) {
            case EndBinaryTag $ -> new GenericListCollector();
            case ListBinaryTag tag -> new GenericListCollector(tag);

            case ByteArrayBinaryTag tag -> new ByteListCollector(tag.value());
            case IntArrayBinaryTag tag -> new IntListCollector(tag.value());
            case LongArrayBinaryTag tag -> new LongListCollector(tag.value());

            default -> null;
        });
    }

    ListCollector accept(BinaryTag input);

    default ListCollector acceptAll(Iterable<BinaryTag> input) {
        ListCollector listCollector = this;

        for (BinaryTag tag : input) {
            listCollector = listCollector.accept(tag);
        }

        return listCollector;
    }

    BinaryTag result();

    final class ByteListCollector implements ListCollector {
        private final ByteArrayList values = new ByteArrayList();

        ByteListCollector(byte[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public ListCollector accept(BinaryTag input) {
            if (input instanceof ByteBinaryTag tag) {
                values.add(tag.value());
                return this;
            }

            return new GenericListCollector(values).accept(input);
        }

        @Override
        public ByteArrayBinaryTag result() {
            return byteArrayBinaryTag(values.toByteArray());
        }
    }

    final class IntListCollector implements ListCollector {
        private final IntArrayList values = new IntArrayList();

        IntListCollector(int[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public ListCollector accept(BinaryTag input) {
            if (input instanceof IntBinaryTag tag) {
                values.add(tag.value());
                return this;
            }

            return new GenericListCollector(values).accept(input);
        }

        @Override
        public IntArrayBinaryTag result() {
            return intArrayBinaryTag(values.toIntArray());
        }
    }

    final class LongListCollector implements ListCollector {
        private final LongArrayList values = new LongArrayList();

        LongListCollector(long[] values) {
            this.values.addElements(0, values);
        }

        @Override
        public ListCollector accept(BinaryTag input) {
            if (input instanceof LongBinaryTag tag) {
                values.add(tag.value());
                return this;
            }

            return new GenericListCollector(values).accept(input);
        }

        @Override
        public LongArrayBinaryTag result() {
            return longArrayBinaryTag(this.values.toLongArray());
        }
    }

    final class GenericListCollector implements ListCollector {
        private final ListBinaryTag.Builder<BinaryTag> result = ListBinaryTag.heterogeneousListBinaryTag();

        GenericListCollector() {
        }

        GenericListCollector(Iterable<BinaryTag> tags) {
            this.result.add(tags);
        }

        GenericListCollector(ByteArrayList data) {
            data.forEach(b -> this.result.add(byteBinaryTag(b)));
        }

        GenericListCollector(IntArrayList data) {
            data.forEach(i -> this.result.add(intBinaryTag(i)));
        }

        GenericListCollector(LongArrayList data) {
            data.forEach(l -> this.result.add(longBinaryTag(l)));
        }

        @Override
        public ListCollector accept(BinaryTag input) {
            result.add(input);
            return this;
        }

        @Override
        public ListBinaryTag result() {
            return result.build();
        }
    }
}
