package com.example.weakspot;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentKeybind;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

/**
 * 遊び方のガイドの本（1.4.1。両側）。サーバーは初めてのログインで渡し、クライアントは統計画面の「ガイド」で開く。
 * ページは翻訳キーの文章なので、読む人のクライアントの言語で出る。キーの名前は、読む人の操作設定から出す。
 * 1.8.5 から、表紙・目次（クリックで飛ぶ）・章ごとのページで、各ページは小見出し（■）で分ける。
 * README の遊び方を変えたら、lang の weakspot.guide.* も合わせて直す（CLAUDE.md）。
 */
public final class GuideBook {

    /** 本の形の版。NBT の TAG_FORMAT。これより古い本は、新しい本に差し替える（1.8.5）。 */
    public static final int FORMAT = 2;
    public static final String TAG_FORMAT = "weakspotGuide";
    private static final String TITLE = "Weak Spot Mining";
    private static final String TOGGLE_KEY = "key.weakspot.toggle";
    private static final String STATS_KEY = "key.weakspot.stats";

    /** 章。見出しの色は、本の紙（ベージュ）で読める濃い色だけ。 */
    private enum Chapter {
        BASICS(TextFormatting.DARK_BLUE),
        MINING(TextFormatting.DARK_GREEN),
        RIGHT_CLICK(TextFormatting.DARK_RED),
        AIM(TextFormatting.DARK_PURPLE),
        SCREEN(TextFormatting.DARK_AQUA),
        MORE(TextFormatting.GOLD);

        final TextFormatting color;

        Chapter(TextFormatting color) {
            this.color = color;
        }
    }

    /** 小見出しと本文。本文は weakspot.guide.<ページ>.<name>、小見出しは weakspot.guide.head.<name>。 */
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

    /** 本文のページの並び（章の順）。 */
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
            new Page("fishing", Chapter.SCREEN, s("when"), s("hit")),
            new Page("sleep", Chapter.SCREEN, s("when"), s("hit"), s("tip")),
            new Page("enchant", Chapter.SCREEN, s("when"), s("hit"), s("tip")),
            new Page("multi", Chapter.MORE, s("others"), s("together")),
            new Page("markers", Chapter.MORE, s("onoff", STATS_KEY), s("look")),
            new Page("help", Chapter.MORE, s("busy", TOGGLE_KEY), s("bug")),
    };

    /** 目次の各ページに載せる章（本の 1 ページに 14 行まで）。 */
    private static final Chapter[][] CONTENTS_PAGES = {
            {Chapter.BASICS, Chapter.MINING, Chapter.RIGHT_CLICK},
            {Chapter.AIM},
            {Chapter.SCREEN, Chapter.MORE},
    };
    /** 目次の最初のページ（1 から数える）。 */
    private static final int CONTENTS_FIRST_PAGE = 2;
    /** 本文の最初のページ（1 から数える）。表紙と目次のあと。 */
    private static final int CONTENT_FIRST_PAGE = CONTENTS_FIRST_PAGE + CONTENTS_PAGES.length;

    /** ページ数。表紙・目次・本文。 */
    public static final int PAGES = CONTENT_FIRST_PAGE - 1 + CONTENT.length;

    private GuideBook() {
    }

    /** 記入済みの本。 */
    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", TITLE);
        tag.setString("author", TITLE);
        // 読むときにサーバーが中身を解決し直さない（翻訳キーはクライアントで訳す）
        tag.setBoolean("resolved", true);
        tag.setInteger(TAG_FORMAT, FORMAT);
        NBTTagList pages = new NBTTagList();
        for (ITextComponent page : pages()) {
            pages.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(page)));
        }
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }

    /** この Mod が渡した、古い形のガイドの本か（1.8.5 より前の本は、番号の翻訳キーで、今の lang にはない）。 */
    public static boolean isOutdated(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.WRITTEN_BOOK || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return TITLE.equals(tag.getString("title")) && TITLE.equals(tag.getString("author"))
                && tag.getInteger(TAG_FORMAT) < FORMAT;
    }

    /** 古い本の代わり（数はそのまま）。 */
    public static ItemStack replacement(ItemStack old) {
        ItemStack book = create();
        book.setCount(old.getCount());
        return book;
    }

    private static List<ITextComponent> pages() {
        List<ITextComponent> pages = new ArrayList<>();
        pages.add(cover());
        for (Chapter[] chapters : CONTENTS_PAGES) {
            pages.add(contents(chapters));
        }
        for (Page page : CONTENT) {
            pages.add(content(page));
        }
        return pages;
    }

    private static ITextComponent cover() {
        ITextComponent page = new TextComponentString("");
        page.appendSibling(styled(new TextComponentTranslation("weakspot.guide.cover.title"),
                TextFormatting.DARK_BLUE, true));
        page.appendSibling(new TextComponentString("\n"));
        page.appendSibling(styled(new TextComponentTranslation("weakspot.guide.cover.subtitle"),
                TextFormatting.DARK_GRAY, false));
        page.appendSibling(new TextComponentString("\n\n"));
        page.appendSibling(new TextComponentTranslation("weakspot.guide.cover.text"));
        page.appendSibling(new TextComponentString("\n\n"));
        page.appendSibling(link(new TextComponentTranslation("weakspot.guide.cover.contents"), CONTENTS_FIRST_PAGE));
        return page;
    }

    private static ITextComponent contents(Chapter[] chapters) {
        ITextComponent page = new TextComponentString("");
        page.appendSibling(styled(new TextComponentTranslation("weakspot.guide.contents"), TextFormatting.BLACK, true));
        for (Chapter chapter : chapters) {
            page.appendSibling(new TextComponentString("\n"));
            page.appendSibling(styled(chapterTitle(chapter), chapter.color, true));
            for (int i = 0; i < CONTENT.length; i++) {
                if (CONTENT[i].chapter != chapter) {
                    continue;
                }
                int number = CONTENT_FIRST_PAGE + i;
                ITextComponent entry = new TextComponentString(" ");
                entry.appendSibling(new TextComponentTranslation("weakspot.guide." + CONTENT[i].id + ".title"));
                entry.appendSibling(styled(new TextComponentString(" " + number), TextFormatting.DARK_GRAY, false));
                page.appendSibling(new TextComponentString("\n"));
                page.appendSibling(link(entry, number));
            }
        }
        return page;
    }

    /** 本文のページ: 章の色の太字の題、小見出しと本文、最後に目次へのリンク（1 ページは 14 行なので、空行は入れない）。 */
    private static ITextComponent content(Page p) {
        ITextComponent page = new TextComponentString("");
        page.appendSibling(styled(new TextComponentTranslation("weakspot.guide." + p.id + ".title"),
                p.chapter.color, true));
        for (Section section : p.sections) {
            page.appendSibling(new TextComponentString("\n"));
            page.appendSibling(styled(new TextComponentTranslation("weakspot.guide.head." + section.name),
                    TextFormatting.DARK_GRAY, false));
            page.appendSibling(new TextComponentString("\n"));
            Object[] args = section.keybind == null ? new Object[0] : new Object[] {keybind(section.keybind)};
            page.appendSibling(new TextComponentTranslation("weakspot.guide." + p.id + "." + section.name, args));
        }
        page.appendSibling(new TextComponentString("\n"));
        page.appendSibling(link(new TextComponentTranslation("weakspot.guide.back"), CONTENTS_FIRST_PAGE));
        return page;
    }

    private static ITextComponent chapterTitle(Chapter chapter) {
        return new TextComponentTranslation("weakspot.guide.chapter." + (chapter.ordinal() + 1));
    }

    private static ITextComponent keybind(String key) {
        ITextComponent component = new TextComponentKeybind(key);
        component.getStyle().setBold(true);
        return component;
    }

    private static ITextComponent styled(ITextComponent component, TextFormatting color, boolean bold) {
        component.getStyle().setColor(color).setBold(bold);
        return component;
    }

    /** クリックで page ページ目（1 から数える）へ飛ぶ。 */
    private static ITextComponent link(ITextComponent component, int page) {
        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(page)));
        return component;
    }
}
