package me.glicz.binarytagops;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.kyori.adventure.nbt.*;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static me.glicz.binarytagops.ListCollector.listCollector;
import static net.kyori.adventure.nbt.ByteArrayBinaryTag.byteArrayBinaryTag;
import static net.kyori.adventure.nbt.ByteBinaryTag.byteBinaryTag;
import static net.kyori.adventure.nbt.DoubleBinaryTag.doubleBinaryTag;
import static net.kyori.adventure.nbt.EndBinaryTag.endBinaryTag;
import static net.kyori.adventure.nbt.FloatBinaryTag.floatBinaryTag;
import static net.kyori.adventure.nbt.IntArrayBinaryTag.intArrayBinaryTag;
import static net.kyori.adventure.nbt.IntBinaryTag.intBinaryTag;
import static net.kyori.adventure.nbt.ListBinaryTag.heterogeneousListBinaryTag;
import static net.kyori.adventure.nbt.LongArrayBinaryTag.longArrayBinaryTag;
import static net.kyori.adventure.nbt.LongBinaryTag.longBinaryTag;
import static net.kyori.adventure.nbt.ShortBinaryTag.shortBinaryTag;
import static net.kyori.adventure.nbt.StringBinaryTag.stringBinaryTag;

public final class BinaryTagOps implements DynamicOps<BinaryTag> {
    public static final BinaryTagOps INSTANCE = new BinaryTagOps();

    private BinaryTagOps() {
    }

    @Override
    public BinaryTag empty() {
        return endBinaryTag();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> ops, BinaryTag input) {
        return switch (input) {
            // Numbers
            case ByteBinaryTag tag -> ops.createByte(tag.value());
            case DoubleBinaryTag tag -> ops.createDouble(tag.value());
            case FloatBinaryTag tag -> ops.createFloat(tag.value());
            case IntBinaryTag tag -> ops.createInt(tag.value());
            case LongBinaryTag tag -> ops.createLong(tag.value());
            case ShortBinaryTag tag -> ops.createShort(tag.value());

            // Arrays
            case ByteArrayBinaryTag tag -> ops.createByteList(ByteBuffer.wrap(tag.value()));
            case IntArrayBinaryTag tag -> ops.createIntList(IntStream.of(tag.value()));
            case LongArrayBinaryTag tag -> ops.createLongList(LongStream.of(tag.value()));

            // Other
            case CompoundBinaryTag tag -> this.convertMap(ops, tag);
            case ListBinaryTag tag -> this.convertList(ops, tag);
            case StringBinaryTag tag -> ops.createString(tag.value());
            case EndBinaryTag $ -> ops.empty();

            default -> throw new IllegalStateException("Unknown tag type: " + input);
        };
    }

    @Override
    public DataResult<Number> getNumberValue(BinaryTag input) {
        Number number = switch (input) {
            case ByteBinaryTag tag -> tag.value();
            case DoubleBinaryTag tag -> tag.value();
            case FloatBinaryTag tag -> tag.value();
            case IntBinaryTag tag -> tag.value();
            case LongBinaryTag tag -> tag.value();
            case ShortBinaryTag tag -> tag.value();
            default -> null;
        };

        return number != null ? DataResult.success(number) : DataResult.error(() -> "Not a number");
    }

    @Override
    public BinaryTag createNumeric(Number number) {
        return doubleBinaryTag(number.doubleValue());
    }

    @Override
    public BinaryTag createByte(byte value) {
        return byteBinaryTag(value);
    }

    @Override
    public BinaryTag createDouble(double value) {
        return doubleBinaryTag(value);
    }

    @Override
    public BinaryTag createFloat(float value) {
        return floatBinaryTag(value);
    }

    @Override
    public BinaryTag createInt(int value) {
        return intBinaryTag(value);
    }

    @Override
    public BinaryTag createLong(long value) {
        return longBinaryTag(value);
    }

    @Override
    public BinaryTag createShort(short value) {
        return shortBinaryTag(value);
    }

    @Override
    public DataResult<String> getStringValue(BinaryTag input) {
        return input instanceof StringBinaryTag tag ? DataResult.success(tag.value()) : DataResult.error(() -> "Not a string");
    }

    @Override
    public BinaryTag createString(String value) {
        return stringBinaryTag(value);
    }

    @Override
    public DataResult<BinaryTag> mergeToList(BinaryTag input, BinaryTag value) {
        return mergeToList(input, collector -> collector.accept(value));
    }

    @Override
    public DataResult<BinaryTag> mergeToList(BinaryTag input, List<BinaryTag> values) {
        return mergeToList(input, collector -> collector.acceptAll(values));
    }

    private DataResult<BinaryTag> mergeToList(BinaryTag input, UnaryOperator<ListCollector> operator) {
        return listCollector(input)
                .map(collector -> DataResult.success(operator.apply(collector).result()))
                .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + input, input));
    }

    @Override
    public DataResult<BinaryTag> mergeToMap(BinaryTag map, BinaryTag key, BinaryTag value) {
        if (!(key instanceof StringBinaryTag stringKey)) {
            return DataResult.error(() -> "key is not a string: " + key, map);
        }

        CompoundBinaryTag compoundTag = switch (map) {
            case CompoundBinaryTag tag -> tag;
            case EndBinaryTag $ -> CompoundBinaryTag.empty();
            default -> null;
        };

        if (compoundTag == null) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        }

        return DataResult.success(compoundTag.put(stringKey.value(), value));
    }

    @Override
    public DataResult<BinaryTag> mergeToMap(BinaryTag map, MapLike<BinaryTag> values) {
        return mergeToMap(map, values.entries().iterator(), (pair, consumer) ->
                consumer.accept(pair.getFirst(), pair.getSecond())
        );
    }

    @Override
    public DataResult<BinaryTag> mergeToMap(BinaryTag map, Map<BinaryTag, BinaryTag> values) {
        return mergeToMap(map, values.entrySet().iterator(), (entry, consumer) ->
                consumer.accept(entry.getKey(), entry.getValue())
        );
    }

    private <T> DataResult<BinaryTag> mergeToMap(BinaryTag map, Iterator<T> iterator, BiConsumer<T, BiConsumer<BinaryTag, BinaryTag>> consumer) {
        CompoundBinaryTag.Builder builder = switch (map) {
            case CompoundBinaryTag tag -> CompoundBinaryTag.builder().put(tag);
            case EndBinaryTag $ -> CompoundBinaryTag.builder();
            default -> null;
        };

        if (builder == null) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        }

        List<BinaryTag> unsupportedKeys = new ArrayList<>();

        iterator.forEachRemaining(t -> consumer.accept(t, (rawKey, value) -> {
            if (rawKey instanceof StringBinaryTag key) {
                builder.put(key.value(), value);
            } else {
                unsupportedKeys.add(rawKey);
            }
        }));

        CompoundBinaryTag compoundTag = builder.build();

        return unsupportedKeys.isEmpty()
                ? DataResult.success(compoundTag)
                : DataResult.error(() -> "some keys are not strings: " + unsupportedKeys, compoundTag);
    }

    @Override
    public DataResult<Stream<Pair<BinaryTag, BinaryTag>>> getMapValues(BinaryTag input) {
        if (input instanceof CompoundBinaryTag tag) {
            return DataResult.success(tag.stream().map(entry -> Pair.of(
                    createString(entry.getKey()),
                    entry.getValue())
            ));
        }

        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<Consumer<BiConsumer<BinaryTag, BinaryTag>>> getMapEntries(BinaryTag input) {
        if (input instanceof CompoundBinaryTag tag) {
            return DataResult.success(biConsumer -> {
                for (Map.Entry<String, ? extends BinaryTag> entry : tag) {
                    biConsumer.accept(createString(entry.getKey()), entry.getValue());
                }
            });
        }

        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public DataResult<MapLike<BinaryTag>> getMap(BinaryTag input) {
        if (input instanceof CompoundBinaryTag tag) {
            return DataResult.success(new BinaryTagMapLike(tag));
        }

        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public BinaryTag createMap(Stream<Pair<BinaryTag, BinaryTag>> map) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        map.forEach(pair -> {
            if (pair.getFirst() instanceof StringBinaryTag key) {
                builder.put(key.value(), pair.getSecond());
            } else {
                throw new UnsupportedOperationException("Cannot create map with non-string key: " + pair.getFirst());
            }
        });

        return builder.build();
    }

    @Override
    public DataResult<Stream<BinaryTag>> getStream(BinaryTag input) {
        return switch (input) {
            case ListBinaryTag tag -> DataResult.success(tag.stream());

            case ByteArrayBinaryTag tag -> {
                byte[] bytes = tag.value();

                yield DataResult.success(IntStream.range(0, bytes.length).mapToObj(i -> createByte(bytes[i])));
            }
            case IntArrayBinaryTag tag -> DataResult.success(Arrays.stream(tag.value()).mapToObj(this::createInt));
            case LongArrayBinaryTag tag -> DataResult.success(Arrays.stream(tag.value()).mapToObj(this::createLong));

            default -> DataResult.error(() -> "Not a list");
        };
    }

    @Override
    public DataResult<Consumer<Consumer<BinaryTag>>> getList(BinaryTag input) {
        return switch (input) {
            case ListBinaryTag tag -> DataResult.success(tag::forEach);

            case ByteArrayBinaryTag tag -> DataResult.success(consumer ->
                    tag.forEach(b -> consumer.accept(createByte(b)))
            );
            case IntArrayBinaryTag tag -> DataResult.success(consumer ->
                    tag.forEachInt(i -> consumer.accept(createInt(i)))
            );
            case LongArrayBinaryTag tag -> DataResult.success(consumer ->
                    tag.forEachLong(l -> consumer.accept(createLong(l)))
            );

            default -> DataResult.error(() -> "Not a list: " + input);
        };
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(BinaryTag input) {
        return input instanceof ByteArrayBinaryTag tag ? DataResult.success(ByteBuffer.wrap(tag.value())) : DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public BinaryTag createByteList(ByteBuffer input) {
        ByteBuffer byteBuffer = input.duplicate().clear();
        byte[] bytes = new byte[input.capacity()];
        byteBuffer.get(0, bytes, 0, bytes.length);
        return byteArrayBinaryTag(bytes);
    }

    @Override
    public DataResult<IntStream> getIntStream(BinaryTag input) {
        return input instanceof IntArrayBinaryTag tag ? DataResult.success(Arrays.stream(tag.value())) : DynamicOps.super.getIntStream(input);
    }

    @Override
    public BinaryTag createIntList(IntStream input) {
        return intArrayBinaryTag(input.toArray());
    }

    @Override
    public DataResult<LongStream> getLongStream(BinaryTag input) {
        return input instanceof LongArrayBinaryTag tag ? DataResult.success(Arrays.stream(tag.value())) : DynamicOps.super.getLongStream(input);
    }

    @Override
    public BinaryTag createLongList(LongStream input) {
        return longArrayBinaryTag(input.toArray());
    }

    @Override
    public BinaryTag createList(Stream<BinaryTag> input) {
        return heterogeneousListBinaryTag().add(input::iterator).build();
    }

    @Override
    public BinaryTag remove(BinaryTag input, String key) {
        return input instanceof CompoundBinaryTag tag ? tag.remove(key) : input;
    }

    @Override
    public RecordBuilder<BinaryTag> mapBuilder() {
        return new BinaryTagRecordBuilder();
    }

    private final class BinaryTagMapLike implements MapLike<BinaryTag> {
        private final CompoundBinaryTag tag;

        private BinaryTagMapLike(CompoundBinaryTag tag) {
            this.tag = tag;
        }

        @Override
        public @Nullable BinaryTag get(BinaryTag key) {
            if (key instanceof StringBinaryTag stringKey) {
                return get(stringKey.value());
            }

            throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + tag);
        }

        @Override
        public @Nullable BinaryTag get(String key) {
            return tag.get(key);
        }

        @Override
        public Stream<Pair<BinaryTag, BinaryTag>> entries() {
            return tag.stream().map(entry -> Pair.of(
                    createString(entry.getKey()),
                    entry.getValue())
            );
        }
    }

    private final class BinaryTagRecordBuilder extends RecordBuilder.AbstractStringBuilder<BinaryTag, CompoundBinaryTag.Builder> {
        private BinaryTagRecordBuilder() {
            super(BinaryTagOps.this);
        }

        @Override
        protected CompoundBinaryTag.Builder initBuilder() {
            return CompoundBinaryTag.builder();
        }

        @Override
        protected CompoundBinaryTag.Builder append(String key, BinaryTag value, CompoundBinaryTag.Builder builder) {
            return builder.put(key, value);
        }

        @Override
        protected DataResult<BinaryTag> build(CompoundBinaryTag.Builder builder, @Nullable BinaryTag prefix) {
            if (prefix == null || prefix.type() == BinaryTagTypes.END) {
                return DataResult.success(builder.build());
            }

            if (!(prefix instanceof CompoundBinaryTag tag)) {
                return DataResult.error(() -> "mergeToMap called with not a map: " + prefix, prefix);
            }

            return DataResult.success(tag.put(builder.build()));
        }
    }
}
