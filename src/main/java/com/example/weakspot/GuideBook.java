package com.example.weakspot;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentKeybind;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 遊び方のガイドの本（1.4.1。両側）。サーバーは初めてのログインで渡し、クライアントは統計画面の「ガイド」で開く。
 * ページは翻訳キーの文章なので、読む人のクライアントの言語で出る。キーの名前は、読む人の操作設定から出す。
 * README の遊び方を変えたら、lang の weakspot.guide.* も合わせて直す（CLAUDE.md）。
 */
public final class GuideBook {

    /** ページ数。weakspot.guide.1〜PAGES の title と text が lang にある。 */
    public static final int PAGES = 21;
    private static final String TITLE = "Weak Spot Mining";
    /** キーの名前を出すページと、そのキー（文章の %s の順）。 */
    private static final int KEYS_PAGE = 11;
    private static final String[] KEYS_PAGE_KEYBINDS = {"key.weakspot.toggle", "key.weakspot.stats"};

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
        NBTTagList pages = new NBTTagList();
        for (int n = 1; n <= PAGES; n++) {
            pages.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(page(n))));
        }
        tag.setTag("pages", pages);
        book.setTagCompound(tag);
        return book;
    }

    /** n ページ目: 太字の見出し、空行、本文。 */
    private static ITextComponent page(int n) {
        ITextComponent title = new TextComponentTranslation("weakspot.guide." + n + ".title");
        title.getStyle().setBold(true);
        Object[] args = n == KEYS_PAGE ? keybinds() : new Object[0];
        ITextComponent page = new TextComponentString("");
        page.appendSibling(title);
        page.appendSibling(new TextComponentString("\n\n"));
        page.appendSibling(new TextComponentTranslation("weakspot.guide." + n + ".text", args));
        return page;
    }

    private static Object[] keybinds() {
        Object[] args = new Object[KEYS_PAGE_KEYBINDS.length];
        for (int i = 0; i < args.length; i++) {
            ITextComponent key = new TextComponentKeybind(KEYS_PAGE_KEYBINDS[i]);
            key.getStyle().setBold(true);
            args[i] = key;
        }
        return args;
    }
}
