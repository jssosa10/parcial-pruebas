package android.support.v4.view.animation;

import org.quantumbadger.redreader.common.Constants.FileType;

public class LinearOutSlowInInterpolator extends LookupTableInterpolator {
    private static final float[] VALUES;

    public /* bridge */ /* synthetic */ float getInterpolation(float f) {
        return super.getInterpolation(f);
    }

    static {
        float[] fArr = new float[FileType.IMAGE];
        // fill-array-data instruction
        fArr[0] = 0;
        fArr[1] = 1018551494;
        fArr[2] = 1026403231;
        fArr[3] = 1031476661;
        fArr[4] = 1034053642;
        fArr[5] = 1036375609;
        fArr[6] = 1038603623;
        fArr[7] = 1040475960;
        fArr[8] = 1041522858;
        fArr[9] = 1042536202;
        fArr[10] = 1043522703;
        fArr[11] = 1044482359;
        fArr[12] = 1045415173;
        fArr[13] = 1046327853;
        fArr[14] = 1047220401;
        fArr[15] = 1048092816;
        fArr[16] = 1048760549;
        fArr[17] = 1049179980;
        fArr[18] = 1049589344;
        fArr[19] = 1049988642;
        fArr[20] = 1050381228;
        fArr[21] = 1050767104;
        fArr[22] = 1051146269;
        fArr[23] = 1051518724;
        fArr[24] = 1051884467;
        fArr[25] = 1052240144;
        fArr[26] = 1052592466;
        fArr[27] = 1052941432;
        fArr[28] = 1053280331;
        fArr[29] = 1053615876;
        fArr[30] = 1053944709;
        fArr[31] = 1054266832;
        fArr[32] = 1054585599;
        fArr[33] = 1054901010;
        fArr[34] = 1055209711;
        fArr[35] = 1055511701;
        fArr[36] = 1055810336;
        fArr[37] = 1056105615;
        fArr[38] = 1056397538;
        fArr[39] = 1056682751;
        fArr[40] = 1056964608;
        fArr[41] = 1057103859;
        fArr[42] = 1057239754;
        fArr[43] = 1057375650;
        fArr[44] = 1057508190;
        fArr[45] = 1057639052;
        fArr[46] = 1057768237;
        fArr[47] = 1057895743;
        fArr[48] = 1058019895;
        fArr[49] = 1058144046;
        fArr[50] = 1058266520;
        fArr[51] = 1058385638;
        fArr[52] = 1058504756;
        fArr[53] = 1058622197;
        fArr[54] = 1058737960;
        fArr[55] = 1058850367;
        fArr[56] = 1058962774;
        fArr[57] = 1059073504;
        fArr[58] = 1059182556;
        fArr[59] = 1059291608;
        fArr[60] = 1059397304;
        fArr[61] = 1059501323;
        fArr[62] = 1059605342;
        fArr[63] = 1059707683;
        fArr[64] = 1059808346;
        fArr[65] = 1059907332;
        fArr[66] = 1060004640;
        fArr[67] = 1060101947;
        fArr[68] = 1060197578;
        fArr[69] = 1060291530;
        fArr[70] = 1060383805;
        fArr[71] = 1060476079;
        fArr[72] = 1060566676;
        fArr[73] = 1060655596;
        fArr[74] = 1060742837;
        fArr[75] = 1060830079;
        fArr[76] = 1060915642;
        fArr[77] = 1061001206;
        fArr[78] = 1061083415;
        fArr[79] = 1061165623;
        fArr[80] = 1061247831;
        fArr[81] = 1061326684;
        fArr[82] = 1061405537;
        fArr[83] = 1061484390;
        fArr[84] = 1061561565;
        fArr[85] = 1061637063;
        fArr[86] = 1061710882;
        fArr[87] = 1061784702;
        fArr[88] = 1061858522;
        fArr[89] = 1061928986;
        fArr[90] = 1062001128;
        fArr[91] = 1062069915;
        fArr[92] = 1062138701;
        fArr[93] = 1062207488;
        fArr[94] = 1062274597;
        fArr[95] = 1062340028;
        fArr[96] = 1062405459;
        fArr[97] = 1062469213;
        fArr[98] = 1062532966;
        fArr[99] = 1062595042;
        fArr[100] = 1062655440;
        fArr[101] = 1062717515;
        fArr[102] = 1062776236;
        fArr[103] = 1062834956;
        fArr[104] = 1062893676;
        fArr[105] = 1062950719;
        fArr[106] = 1063006083;
        fArr[107] = 1063061448;
        fArr[108] = 1063116813;
        fArr[109] = 1063170500;
        fArr[110] = 1063224187;
        fArr[111] = 1063276197;
        fArr[112] = 1063326528;
        fArr[113] = 1063378538;
        fArr[114] = 1063427192;
        fArr[115] = 1063477523;
        fArr[116] = 1063524499;
        fArr[117] = 1063573153;
        fArr[118] = 1063620130;
        fArr[119] = 1063665428;
        fArr[120] = 1063710727;
        fArr[121] = 1063756025;
        fArr[122] = 1063799646;
        fArr[123] = 1063843267;
        fArr[124] = 1063885210;
        fArr[125] = 1063927153;
        fArr[126] = 1063967418;
        fArr[127] = 1064007683;
        fArr[128] = 1064047949;
        fArr[129] = 1064086536;
        fArr[130] = 1064125124;
        fArr[131] = 1064162034;
        fArr[132] = 1064198944;
        fArr[133] = 1064235853;
        fArr[134] = 1064271086;
        fArr[135] = 1064306318;
        fArr[136] = 1064339872;
        fArr[137] = 1064373427;
        fArr[138] = 1064406981;
        fArr[139] = 1064438858;
        fArr[140] = 1064470734;
        fArr[141] = 1064500933;
        fArr[142] = 1064531132;
        fArr[143] = 1064561331;
        fArr[144] = 1064589853;
        fArr[145] = 1064618374;
        fArr[146] = 1064646895;
        fArr[147] = 1064673739;
        fArr[148] = 1064700582;
        fArr[149] = 1064727426;
        fArr[150] = 1064752592;
        fArr[151] = 1064777757;
        fArr[152] = 1064802923;
        fArr[153] = 1064826411;
        fArr[154] = 1064849900;
        fArr[155] = 1064871710;
        fArr[156] = 1064893520;
        fArr[157] = 1064915331;
        fArr[158] = 1064937141;
        fArr[159] = 1064957274;
        fArr[160] = 1064977406;
        fArr[161] = 1064995861;
        fArr[162] = 1065014316;
        fArr[163] = 1065032771;
        fArr[164] = 1065051226;
        fArr[165] = 1065068003;
        fArr[166] = 1065084781;
        fArr[167] = 1065099880;
        fArr[168] = 1065116657;
        fArr[169] = 1065131757;
        fArr[170] = 1065145179;
        fArr[171] = 1065160278;
        fArr[172] = 1065173700;
        fArr[173] = 1065185444;
        fArr[174] = 1065198866;
        fArr[175] = 1065210610;
        fArr[176] = 1065222354;
        fArr[177] = 1065232420;
        fArr[178] = 1065242486;
        fArr[179] = 1065252553;
        fArr[180] = 1065262619;
        fArr[181] = 1065271008;
        fArr[182] = 1065279396;
        fArr[183] = 1065287785;
        fArr[184] = 1065296173;
        fArr[185] = 1065302884;
        fArr[186] = 1065309595;
        fArr[187] = 1065314628;
        fArr[188] = 1065321339;
        fArr[189] = 1065326372;
        fArr[190] = 1065331406;
        fArr[191] = 1065334761;
        fArr[192] = 1065339794;
        fArr[193] = 1065343150;
        fArr[194] = 1065344827;
        fArr[195] = 1065348183;
        fArr[196] = 1065349861;
        fArr[197] = 1065351538;
        fArr[198] = 1065351538;
        fArr[199] = 1065353216;
        fArr[200] = 1065353216;
        VALUES = fArr;
    }

    public LinearOutSlowInInterpolator() {
        super(VALUES);
    }
}
