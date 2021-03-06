package garden.ephemeral.minecraft.dozenal;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.Style;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReorderingProcessorMangler extends Mangler<IReorderingProcessor> {
    private final Mangler<String> stringMangler;

    public ReorderingProcessorMangler(Mangler<String> stringMangler) {
        this.stringMangler = stringMangler;
    }

    @Nonnull
    @Override
    protected IReorderingProcessor doMangle(@Nonnull IReorderingProcessor input) {
        return new MangledReorderingProcessor(input);
    }

    private class MangledReorderingProcessor implements IReorderingProcessor {
        private final IReorderingProcessor delegate;
        private IReorderingProcessor mangledDelegate;

        private MangledReorderingProcessor(IReorderingProcessor delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accept(@Nonnull ICharacterConsumer characterConsumer) {
            if (mangledDelegate == null) {
                CoalescingCharacterConsumer coalescingCharacterConsumer = new CoalescingCharacterConsumer();
                delegate.accept(coalescingCharacterConsumer);
                ImmutableList<IReorderingProcessor> runs = coalescingCharacterConsumer.finish();
                mangledDelegate = IReorderingProcessor.func_242241_a(runs);
            }

            return mangledDelegate.accept(characterConsumer);
        }
    }

    private class CoalescingCharacterConsumer implements ICharacterConsumer {
        private final ImmutableList.Builder<IReorderingProcessor> runs = ImmutableList.builder();
        private final StringBuilder currentTextRun = new StringBuilder();
        private @Nullable Style currentStyle = null;

        private void appendCurrentTextRun() {
            if (currentTextRun.length() > 0) {
                String text = stringMangler.mangle(currentTextRun.toString());
                if (currentStyle == null) {
                    throw new AssertionError(); // Impossible as text run length is greater than 0
                }
                runs.add(IReorderingProcessor.fromString(text, currentStyle));
                currentTextRun.setLength(0);
            }
        }

        @Override
        public boolean accept(int index, @Nonnull Style style, int codePoint) {
            if (!style.equals(currentStyle)) {
                appendCurrentTextRun();
                currentStyle = style;
            }

            currentTextRun.appendCodePoint(codePoint);

            // I guess we're forced to always return true here?  Because we don't know whether
            // the string will fit or not after we mangle it.
            return true;
        }

        public ImmutableList<IReorderingProcessor> finish() {
            appendCurrentTextRun();
            return runs.build();
        }
    }
}
