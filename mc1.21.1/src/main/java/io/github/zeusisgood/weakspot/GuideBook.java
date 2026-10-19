package io.github.zeusisgood.weakspot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * ページは翻訳キーの文章なので、読む人のクライアントの言語で出る。キーの名前は、読む人の操作設定から出す。
 */
public final class GuideBook {

    private static final String TITLE = "Weak Spot Mining";
    private static final String TOGGLE_KEY = "key.weakspot.toggle";
    private static final String STATS_KEY = "key.weakspot.stats";

    /** 見出しの色は、本の紙（ベージュ）で読める濃い色だけ。 */
    private enum Chapter {
        BASICS(ChatFormatting.DARK_BLUE),
        MINING(ChatFormatting.DARK_GREEN),
        RIGHT_CLICK(ChatFormatting.DARK_RED),
        AIM(ChatFormatting.DARK_PURPLE),
        SCREEN(ChatFormatting.DARK_AQUA),
        MORE(ChatFormatting.GOLD);

        final ChatFormatting color;

        Chapter(ChatFormatting color) {
            this.color = color;
        }
    }

    /** 本文は weakspot.guide.<ページ>.<name>、小見出しは weakspot.guide.head.<name>。 */
    private static final class Section {
        final String name;
        /** 本文の %s に入れるキー（なければ null）。 */
        final String keybind;

        Section(String name, String keybind) {
            this.name = name;
            this.keybind = keybind;
        }
    }

    private static final class Page {
        final String id;
        final Chapter chapter;
        final Section[] sections;

        Page(String id, Chapter chapter, Section... sections) {
            this.id = id;
            this.chapter = chapter;
            this.sections = sections;
        }
    }

    private static Section s(String name) {
        return new Section(name, null);
    }

    private static Section s(String name, String keybind) {
        return new Section(name, keybind);
    }

    private static final Page[] CONTENT = {
            new Page("keys", Chapter.BASICS, s("toggle", TOGGLE_KEY), s("menu", STATS_KEY)),
            new Page("combo", Chapter.BASICS, s("combo"), s("sound")),
            new Page("mining", Chapter.MINING, s("when"), s("hit"), s("tip")),
            new Page("repair", Chapter.MINING, s("repair"), s("milestone")),
            new Page("growth", Chapter.RIGHT_CLICK, s("when"), s("hit"), s("tip")),
            new Page("harvest", Chapter.RIGHT_CLICK, s("when"), s("hit"), s("tip")),
            new Page("machine", Chapter.RIGHT_CLICK, s("when"), s("hit"), s("tip")),
            new Page("animal", Chapter.RIGHT_CLICK, s("when"), s("hit"), s("tip")),
            new Page("aim", Chapter.AIM, s("place"), s("aim"), s("tip")),
            new Page("bow", Chapter.AIM, s("when"), s("hit"), s("tip")),
            new Page("melee", Chapter.AIM, s("when"), s("hit"), s("tip")),
            new Page("throw", Chapter.AIM, s("when"), s("hit"), s("tip")),
            new Page("eat", Chapter.AIM, s("when"), s("hit")),
            new Page("vehicle", Chapter.AIM, s("when"), s("hit"), s("tip")),
            new Page("ladder", Chapter.AIM, s("when"), s("hit")),
            new Page("sprint", Chapter.AIM, s("when"), s("hit")),
            new Page("elytra", Chapter.AIM, s("when"), s("hit"), s("tip")),
            new Page("portal", Chapter.AIM, s("when"), s("hit")),
            new Page("swim", Chapter.AIM, s("when"), s("hit")),
            new Page("fall", Chapter.AIM, s("when"), s("hit")),
            new Page("fishing", Chapter.SCREEN, s("when"), s("hit")),
            new Page("sleep", Chapter.SCREEN, s("when"), s("hit"), s("tip")),
            new Page("enchant", Chapter.SCREEN, s("when"), s("hit"), s("tip")),
            new Page("target", Chapter.MORE, s("how"), s("recipe")),
            new Page("targetrule", Chapter.MORE, s("rule"), s("reward")),
            new Page("multi", Chapter.MORE, s("others"), s("together")),
            new Page("markers", Chapter.MORE, s("onoff", STATS_KEY), s("look")),
            new Page("help", Chapter.MORE, s("busy", TOGGLE_KEY), s("bug"), s("update")),
    };

    /** 本の 1 ページに 14 行まで。 */
    private static final Chapter[][] CONTENTS_PAGES = {
            {Chapter.BASICS, Chapter.MINING, Chapter.RIGHT_CLICK},
            {Chapter.AIM},
            {Chapter.SCREEN, Chapter.MORE},
    };
    /** 1 から数える。 */
    private static final int CONTENTS_FIRST_PAGE = 2;
    private static final int CONTENT_FIRST_PAGE = CONTENTS_FIRST_PAGE + CONTENTS_PAGES.length;

    private GuideBook() {
    }

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (Component page : pages()) {
            pages.add(Filterable.passThrough(page));
        }
        // resolved: 読むときにサーバーが中身を解決し直さない（翻訳キーはクライアントで訳す）
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(TITLE), TITLE, 0, pages, true));
        return book;
    }

    private static List<Component> pages() {
        List<Component> pages = new ArrayList<>();
        pages.add(cover());
        for (Chapter[] chapters : CONTENTS_PAGES) {
            pages.add(contents(chapters));
        }
        for (Page page : CONTENT) {
            pages.add(content(page));
        }
        return pages;
    }

    private static MutableComponent cover() {
        MutableComponent page = Component.empty();
        page.append(styled(Component.translatable("weakspot.guide.cover.title"),
                ChatFormatting.DARK_BLUE, true));
        page.append(Component.literal("\n"));
        page.append(styled(Component.translatable("weakspot.guide.cover.subtitle"),
                ChatFormatting.DARK_GRAY, false));
        page.append(Component.literal("\n\n"));
        page.append(Component.translatable("weakspot.guide.cover.text"));
        page.append(Component.literal("\n\n"));
        page.append(link(Component.translatable("weakspot.guide.cover.contents"), CONTENTS_FIRST_PAGE));
        return page;
    }

    private static MutableComponent contents(Chapter[] chapters) {
        MutableComponent page = Component.empty();
        page.append(styled(Component.translatable("weakspot.guide.contents"), ChatFormatting.BLACK, true));
        for (Chapter chapter : chapters) {
            page.append(Component.literal("\n"));
            page.append(styled(chapterTitle(chapter), chapter.color, true));
            for (int i = 0; i < CONTENT.length; i++) {
                if (CONTENT[i].chapter != chapter) {
                    continue;
                }
                int number = CONTENT_FIRST_PAGE + i;
                MutableComponent entry = Component.literal(" ");
                entry.append(Component.translatable("weakspot.guide." + CONTENT[i].id + ".title"));
                entry.append(styled(Component.literal(" " + number), ChatFormatting.DARK_GRAY, false));
                page.append(Component.literal("\n"));
                page.append(link(entry, number));
            }
        }
        return page;
    }

    /** 1 ページは 14 行なので、空行は入れない。 */
    private static MutableComponent content(Page p) {
        MutableComponent page = Component.empty();
        page.append(styled(Component.translatable("weakspot.guide." + p.id + ".title"),
                p.chapter.color, true));
        for (Section section : p.sections) {
            page.append(Component.literal("\n"));
            page.append(styled(Component.translatable("weakspot.guide.head." + section.name),
                    ChatFormatting.DARK_GRAY, false));
            page.append(Component.literal("\n"));
            Object[] args = section.keybind == null ? new Object[0] : new Object[] {keybind(section.keybind)};
            page.append(Component.translatable("weakspot.guide." + p.id + "." + section.name, args));
        }
        page.append(Component.literal("\n"));
        page.append(link(Component.translatable("weakspot.guide.back"), CONTENTS_FIRST_PAGE));
        return page;
    }

    private static MutableComponent chapterTitle(Chapter chapter) {
        return Component.translatable("weakspot.guide.chapter." + (chapter.ordinal() + 1));
    }

    private static MutableComponent keybind(String key) {
        return Component.keybind(key).withStyle(ChatFormatting.BOLD);
    }

    private static MutableComponent styled(MutableComponent component, ChatFormatting color, boolean bold) {
        return component.withStyle(style -> style.withColor(color).withBold(bold));
    }

    /** page は 1 から数える。 */
    private static MutableComponent link(MutableComponent component, int page) {
        return component.withStyle(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(page))));
    }
}
