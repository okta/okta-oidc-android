#!/bin/bash

PATH_TO_BASEDIR=$1

export JAVA_RUNFILES="${PATH_TO_BASEDIR}/desugar/Desugar.runfiles"
DESUGAR="${PATH_TO_BASEDIR}/desugar/Desugar"

readonly DESUGAR_JAVA8_LIBS_CONFIG=(--rewrite_core_library_prefix java/time/ \
    --rewrite_core_library_prefix java/lang/Double8 \
    --rewrite_core_library_prefix java/lang/Integer8 \
    --rewrite_core_library_prefix java/lang/Long8 \
    --rewrite_core_library_prefix java/lang/Math8 \
    --rewrite_core_library_prefix java/util/stream/ \
    --rewrite_core_library_prefix java/util/function/ \
    --rewrite_core_library_prefix java/util/Desugar \
    --rewrite_core_library_prefix java/util/DoubleSummaryStatistics \
    --rewrite_core_library_prefix java/util/IntSummaryStatistics \
    --rewrite_core_library_prefix java/util/LongSummaryStatistics \
    --rewrite_core_library_prefix java/util/Objects \
    --rewrite_core_library_prefix java/util/Optional \
    --rewrite_core_library_prefix java/util/PrimitiveIterator \
    --rewrite_core_library_prefix java/util/Spliterator \
    --rewrite_core_library_prefix java/util/StringJoiner \
    --rewrite_core_library_prefix java/util/concurrent/ConcurrentHashMap \
    --rewrite_core_library_prefix java/util/concurrent/ThreadLocalRandom \
    --rewrite_core_library_prefix java/util/concurrent/atomic/DesugarAtomic \
    --retarget_core_library_member "java/lang/Double#max->java/lang/Double8" \
    --retarget_core_library_member "java/lang/Double#min->java/lang/Double8" \
    --retarget_core_library_member "java/lang/Double#sum->java/lang/Double8" \
    --retarget_core_library_member "java/lang/Integer#max->java/lang/Integer8" \
    --retarget_core_library_member "java/lang/Integer#min->java/lang/Integer8" \
    --retarget_core_library_member "java/lang/Integer#sum->java/lang/Integer8" \
    --retarget_core_library_member "java/lang/Long#max->java/lang/Long8" \
    --retarget_core_library_member "java/lang/Long#min->java/lang/Long8" \
    --retarget_core_library_member "java/lang/Long#sum->java/lang/Long8" \
    --retarget_core_library_member "java/lang/Math#toIntExact->java/lang/Math8" \
    --retarget_core_library_member "java/util/Arrays#stream->java/util/DesugarArrays" \
    --retarget_core_library_member "java/util/Arrays#spliterator->java/util/DesugarArrays" \

    --retarget_core_library_member "java/util/ServiceLoader#spliterator->java/util/DesugarServiceLoader" \

    --retarget_core_library_member "java/util/Calendar#toInstant->java/util/DesugarCalendar" \
    --retarget_core_library_member "java/util/Date#from->java/util/DesugarDate" \
    --retarget_core_library_member "java/util/Date#toInstant->java/util/DesugarDate" \
    --retarget_core_library_member "java/util/GregorianCalendar#from->java/util/DesugarGregorianCalendar" \
    --retarget_core_library_member "java/util/GregorianCalendar#toZonedDateTime->java/util/DesugarGregorianCalendar" \
    --retarget_core_library_member "java/util/LinkedHashSet#spliterator->java/util/DesugarLinkedHashSet" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicInteger#getAndUpdate->java/util/concurrent/atomic/DesugarAtomicInteger" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicInteger#updateAndGet->java/util/concurrent/atomic/DesugarAtomicInteger" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicInteger#getAndAccumulate->java/util/concurrent/atomic/DesugarAtomicInteger" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicInteger#accumulateAndGet->java/util/concurrent/atomic/DesugarAtomicInteger" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicLong#getAndUpdate->java/util/concurrent/atomic/DesugarAtomicLong" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicLong#updateAndGet->java/util/concurrent/atomic/DesugarAtomicLong" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicLong#getAndAccumulate->java/util/concurrent/atomic/DesugarAtomicLong" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicLong#accumulateAndGet->java/util/concurrent/atomic/DesugarAtomicLong" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicReference#getAndUpdate->java/util/concurrent/atomic/DesugarAtomicReference" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicReference#updateAndGet->java/util/concurrent/atomic/DesugarAtomicReference" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicReference#getAndAccumulate->java/util/concurrent/atomic/DesugarAtomicReference" \
    --retarget_core_library_member "java/util/concurrent/atomic/AtomicReference#accumulateAndGet->java/util/concurrent/atomic/DesugarAtomicReference" \
    --emulate_core_library_interface java/util/Collection \
    --emulate_core_library_interface java/util/Map \
    --emulate_core_library_interface java/util/Map\$Entry \
    --emulate_core_library_interface java/util/Iterator \
    --emulate_core_library_interface java/util/Comparator \
    --dont_rewrite_core_library_invocation "java/util/Iterator#remove" )

readonly TMPDIR="${PATH_TO_BASEDIR}/tmp"
$(rm -rf "${TMPDIR}")
$(mkdir "${TMPDIR}")

params="${TMPDIR}/desugar.params"
for i in $*; do
    if [ $i == $1 ]; then
      echo " "
    else
      echo $i >> "${params}"
    fi
done

echo "--desugar_supported_core_libs" >> "${params}"

for o in "${DESUGAR_JAVA8_LIBS_CONFIG[@]}"; do
    echo "${o}" >> "${params}"
done

 "${DESUGAR}" \
        "--jvm_flag=-XX:+IgnoreUnrecognizedVMOptions" \
        "--jvm_flags=--add-opens=java.base/java.lang.invoke=ALL-UNNAMED" \
        "--jvm_flags=--add-opens=java.base/java.nio=ALL-UNNAMED" \
        "--jvm_flags=--add-opens=java.base/java.lang=ALL-UNNAMED" \
        "--jvm_flag=-Djdk.internal.lambda.dumpProxyClasses=${TMPDIR}" \
        "@${params}"


$(rm -rf "${TMPDIR}")

