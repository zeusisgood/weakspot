package io.github.zeusisgood.weakspot.config;

import io.github.zeusisgood.weakspot.common.HitScale;

import io.github.zeusisgood.weakspot.common.MarkerShape;
import net.minecraftforge.common.config.Config;

/**
 * [クライアント] の設定（weakspot.cfg の client）。各プレイヤーの設定のまま使う。
 * Forge の @Config の決まりで、入れ子のカテゴリの項目は static ではないインスタンスのフィールドにする。
 */
public final class ClientConfig {

    @Config.Comment("音")
    public final Sound sound = new Sound();

    @Config.Comment("マーカー")
    public final Markers markers = new Markers();

    @Config.Comment("コンボ")
    public final Combo combo = new Combo();

    @Config.Comment("ゲージ・表示")
    public final Hud hud = new Hud();

    @Config.Comment("更新")
    public final Updates updates = new Updates();

    /** 音（client.sound） */
    public static final class Sound {

        @Config.Comment({"[クライアント] 自分のヒット音に使うノートブロックの楽器（採掘・成長・機械で共通）",
                "XYLOPHONE, CHIME, BELL, FLUTE, GUITAR, HARP, BASS, HAT, SNARE, BASEDRUM, PLING",
                "統計画面（K キー）のサウンドでも変えられ、試聴できる"})
        public HitSound myHitSound = HitSound.PLING;

        @Config.Comment({"[クライアント] 自分のヒット音の音量（0 で聞こえなくなる）",
                "バニラの「プレイヤー」音量も掛かる"})
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double myHitVolume = 0.25;

        @Config.Comment({"[クライアント] 自分のヒット音に、コンボで音を重ねるか",
                "オンなら、コンボ 25 から 2 音、100 から 3 音の和音になる。オフなら 1 音のまま"})
        public boolean hitChordEnabled = true;

        @Config.Comment({"[クライアント] ヒット音の音階の動き",
                "UP: 上がりきったら最低音に戻る　UP_DOWN: 上がったら下がる往復",
                "他のプレイヤーのヒット音にも、この設定を使う"})
        public HitScale.Direction hitScaleDirection = HitScale.Direction.UP;

        @Config.Comment({"[クライアント] ヒット音の音階の種類",
                "MAJOR: 長音階　PENTATONIC: ペンタトニック（ド レ ミ ソ ラ）　MINOR: 短調",
                "コンボの節目の駆け上がりと、途切れたときの音にも使う"})
        public HitScale.Type hitScaleType = HitScale.Type.MAJOR;

        @Config.Comment({"[クライアント] ヒット音の音域（オクターブ）",
                "1: ピッチ 1.0〜2.0　2: 1 つ下のオクターブ（0.5）から 2.0 まで"})
        @Config.RangeInt(min = HitScale.MIN_OCTAVES, max = HitScale.MAX_OCTAVES)
        public int hitScaleOctaves = 1;

        @Config.Comment({"[クライアント] 他のプレイヤーのヒット音に使うノートブロックの楽器",
                "XYLOPHONE, CHIME, BELL, FLUTE, GUITAR, HARP, BASS, HAT, SNARE, BASEDRUM, PLING"})
        public HitSound othersHitSound = HitSound.XYLOPHONE;

        @Config.Comment({"[クライアント] 他のプレイヤーのヒット音の音量（0 で聞こえなくなる）",
                "バニラの「プレイヤー」音量も掛かる"})
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double othersHitVolume = 0.4;

        Sound() {
        }
    }

    /** マーカー（client.markers） */
    public static final class Markers {

        @Config.Comment({"[クライアント] 自分の弱点のオン・オフ（HOME キーで切り替わる。操作設定で変えられる）",
                "オフの間は、自分の弱点が出ず、通常の遊び方になる。他のプレイヤーのマークは見える"})
        public boolean weakSpotsEnabled = true;

        @Config.Comment({"[クライアント] 自分でオフにした弱点の種類（統計画面の「弱点マーカー」タブで変えられる）",
                "1 行に 1 つ、mining, growth, machine, animal, fishing, bow, melee, vehicle, eat, sleep, ladder, elytra,",
                "enchant, harvest, throw, sprint, portal, swim, fall のどれか。オフの種類は弱点が出ず、バニラの動きになる"})
        public String[] disabledKinds = {};

        @Config.Comment({"[クライアント] 自分の弱点の色。1 行に「種類=#RRGGBB」（例: harvest=#FF3DCB）",
                "書いていない種類は初期値の色。統計画面の「弱点マーカー」タブで変えられる"})
        public String[] myMarkerColors = {};

        @Config.Comment({"[クライアント] 自分の弱点の形。1 行に「種類=形」（例: bow=diamond）",
                "形は circle, ring, diamond, square。書いていない種類は円。統計画面の「弱点マーカー」タブで変えられる"})
        public String[] myMarkerShapes = {};

        @Config.Comment({"[クライアント] 弱点に当てたとき、金の粒を散らすか（的当ての金のご褒美。届いていないワールドでは出ない）"})
        public boolean goldHitParticles = false;

        @Config.Comment({"[クライアント] 弱点が移動するときの演出（古い位置から素早く動き、残像を残す）",
                "見た目だけで、当たり判定は移動先で即時。オフにすると、その場で切り替わる。他のプレイヤーのマークにも効く"})
        public boolean weakSpotTrailEnabled = true;

        @Config.Comment({"[クライアント] 自分の動物・敵の弱点が体に隠れたとき、隠れた部分を薄く透かして表示するか",
                "ニワトリやゾンビの腕など、体が当たり判定より大きいときに見やすくなる。他のプレイヤーのマークは透かさない"})
        public boolean animalSpotSeeThrough = true;

        @Config.Comment("[クライアント] 他のプレイヤーの弱点マークを表示するか")
        public boolean otherMarkerEnabled = true;

        @Config.Comment({"[クライアント] 他のプレイヤーの弱点マークの色（#RRGGBB）。自分のマーク（オレンジ）と区別できる色にする",
                "読めない値のときは初期値の水色 #3FA9FF を使う"})
        public String otherMarkerColor = "#3FA9FF";

        @Config.Comment("[クライアント] 他のプレイヤーの弱点マークの濃さ（自分のマークの濃さに掛ける。1 で同じ、0 で見えない）")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double otherMarkerAlpha = 0.6;

        @Config.Comment({"[クライアント] 他のプレイヤーの弱点マークの形",
                "CIRCLE（塗りつぶした円）, RING（中抜きの輪）, DIAMOND（ひし形）, SQUARE（四角）。自分のマークは円のまま"})
        public MarkerShape otherMarkerShape = MarkerShape.RING;

        Markers() {
        }
    }

    /** コンボ（client.combo） */
    public static final class Combo {

        @Config.Comment({"[クライアント] 連続ヒット（コンボ）の数を画面に表示するか",
                "ヒット音の音階と同じ数え方（約2秒ヒットがないと途切れる）。2 以上で表示する"})
        public boolean comboDisplayEnabled = true;

        @Config.Comment("[クライアント] コンボの数字の大きさの倍率")
        @Config.RangeDouble(min = 0.5, max = 2.0)
        public double comboScale = 1.0;

        @Config.Comment({"[クライアント] コンボの表示の位置",
                "BELOW_CROSSHAIR（照準の下）, RIGHT_OF_CROSSHAIR（照準の右）, TOP_CENTER（画面の上の中央）"})
        public ComboPosition comboPosition = ComboPosition.BELOW_CROSSHAIR;

        @Config.Comment("[クライアント] コンボが 10、25、50、75、100、150、200、250、300（以降 100 ごと）に達したときの演出（強調音・光・花火・タイトル）")
        public boolean comboMilestoneEffects = true;

        @Config.Comment({"[クライアント] 近くのほかのプレイヤーのコンボ（10 以上）を、頭の上に「n HIT」と表示するか"})
        public boolean othersComboDisplay = true;

        @Config.Comment({"[クライアント] コンボの数字の下に、次に掛け数が上がる段階（25・50・100…）までの進み具合のゲージを表示するか",
                "コンボの掛け数を使う種類の弱点が出ている間だけ出る。色は次の段階の色"})
        public boolean comboFactorGaugeEnabled = true;

        @Config.Comment({"[クライアント] ほかのプレイヤーが節目に届いたときの知らせを、チャットに表示するか",
                "オフにすると、ほかの人の節目の知らせが出ない（自分の節目の演出とチャットは出る）"})
        public boolean showOthersMilestones = true;

        Combo() {
        }
    }

    /** ゲージ・表示（client.hud） */
    public static final class Hud {

        @Config.Comment({"[クライアント] 掘っているブロックの残りの耐久を、面の下の余白に緑のバーで表示するか",
                "自分が左クリックの長押しで掘っている、弱点が出るブロックだけに出る"})
        public boolean blockHealthBarEnabled = true;

        @Config.Comment({"[クライアント] かまど・醸造台・スポナーの上に、進み具合（黄）と燃料（橙）のバーを表示するか",
                "機械の弱点を出している間（しゃがんで素手で右クリックを押しっぱなしにしている間）だけ出る"})
        public boolean machineBarEnabled = true;

        @Config.Comment({"[クライアント] 作物の足元に、成長の進み具合を黄色のバーで表示するか",
                "成長の弱点を出している間（素手で右クリックを押しっぱなしにしている間）だけ出る"})
        public boolean growthBarEnabled = true;

        @Config.Comment({"[クライアント] 加速中の機械のまわりの色の粒子を、自分の画面に出すか",
                "自分の機械の粒子も、ほかのプレイヤーの機械の粒子も、同じ設定で消える"})
        public boolean machineParticlesVisible = true;

        @Config.Comment({"[クライアント] 弓を引いている間、照準の下に引き具合のゲージを表示するか",
                "弓の弱点や、弱点の一時オフ（HOME キー）に関係なく出る"})
        public boolean bowDrawBarEnabled = true;

        @Config.Comment({"[クライアント] 乗り物を加速している間、照準の上に残り時間のゲージ（水色）を表示するか"})
        public boolean vehicleBoostBarEnabled = true;

        @Config.Comment({"[クライアント] はしごを加速している間、照準の上に残り時間のゲージ（茶色）を表示するか"})
        public boolean ladderBoostBarEnabled = true;

        @Config.Comment({"[クライアント] 投擲物の溜めのゲージ（青緑）を、照準の下に表示するか"})
        public boolean throwChargeBarEnabled = true;

        @Config.Comment({"[クライアント] 近接の溜めのゲージ（銀）を、照準の下に表示するか"})
        public boolean meleeChargeBarEnabled = true;

        @Config.Comment({"[クライアント] ダッシュが加速している間、照準の上に残り時間のゲージ（赤）を表示するか"})
        public boolean sprintBoostBarEnabled = true;

        @Config.Comment({"[クライアント] 泳ぎが加速している間、照準の上に残り時間のゲージ（深い青）を表示するか"})
        public boolean swimBoostBarEnabled = true;

        @Config.Comment({"[クライアント] 落ちている間、照準の上に、着地したときの見込みのダメージ（ハート）を表示するか"})
        public boolean fallDamageHintEnabled = true;

        Hud() {
        }
    }

    /** 更新（client.updates） */
    public static final class Updates {

        @Config.Comment("[クライアント] 版が変わって初めてワールドに入ったときに、チャットに更新のお知らせを出すか")
        public boolean showUpdateNotes = true;

        @Config.Comment("[クライアント] 最後に更新のお知らせを見た版。Mod が書き換えます。書き換えないでください")
        public String lastSeenVersion = "";

        @Config.Comment("[クライアント] 新しい版が公開されていたら、ワールドに入ったときにチャットで知らせるか。"
                + "Forge の versionCheck が false のときは出ない")
        public boolean checkForUpdates = true;

        @Config.Comment("[クライアント] 「この版は通知しない」を押した版。この版の通知は出さない。空にすると、また出る")
        public String skippedUpdateVersion = "";

        Updates() {
        }
    }

    ClientConfig() {
    }
}
