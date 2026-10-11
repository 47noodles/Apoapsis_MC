package dev.apoapsis.ages;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * The Custodian's guide as a vanilla written book: one page per body line of each {@link Guide.Step}, under the step's
 * header, because a book page holds about fourteen short rows and a step's two lines would not fit together. The text
 * comes from the same lang keys as the chat steps, so the book and the chat always say the same thing.
 */
final class GuideBook {
    private GuideBook() {}

    /** The number of pages in the book. */
    static final int PAGES = Guide.TOTAL * Guide.LINES;

    /** A new written book holding every step of the guide, in order. */
    static ItemStack create() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (Guide.Step step : Guide.Step.values()) {
            for (int line = 1; line <= Guide.LINES; line++) {
                pages.add(Filterable.passThrough(page(step, line)));
            }
        }
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough(Component.translatable("apoapsis.guide.book.title").getString()),
                Component.translatable("apoapsis.guide.book.author").getString(),
                0, List.copyOf(pages), true);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    /** One page: the step's bold header, a blank line, then one body line. Only the header is bold. */
    private static Component page(Guide.Step step, int line) {
        MutableComponent page = Component.empty();
        page.append(Component.translatable("apoapsis.guide.header", String.valueOf(step.number()),
                String.valueOf(Guide.TOTAL), Component.translatable(step.titleKey())).withStyle(ChatFormatting.BOLD));
        page.append(Component.literal("\n\n"));
        page.append(Component.translatable(step.lineKey(line), step.args()));
        return page;
    }
}
