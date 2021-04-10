package net.smackem.zlang.emit.ir;

public enum Register {
    R000(0),
    R001(1),
    R002(2),
    R003(3),
    R004(4),
    R005(5),
    R006(6),
    R007(7),
    R008(8),
    R009(9),
    R010(10),
    R011(11),
    R012(12),
    R013(13),
    R014(14),
    R015(15),
    R016(16),
    R017(17),
    R018(18),
    R019(19),
    R020(20),
    R021(21),
    R022(22),
    R023(23),
    R024(24),
    R025(25),
    R026(26),
    R027(27),
    R028(28),
    R029(29),
    R030(30),
    R031(31),
    R032(32),
    R033(33),
    R034(34),
    R035(35),
    R036(36),
    R037(37),
    R038(38),
    R039(39),
    R040(40),
    R041(41),
    R042(42),
    R043(43),
    R044(44),
    R045(45),
    R046(46),
    R047(47),
    R048(48),
    R049(49),
    R050(50),
    R051(51),
    R052(52),
    R053(53),
    R054(54),
    R055(55),
    R056(56),
    R057(57),
    R058(58),
    R059(59),
    R060(60),
    R061(61),
    R062(62),
    R063(63),
    R064(64);

    private final int number;

    Register(int number) {
        this.number = number;
    }

    public int number() {
        return this.number;
    }

    public static Register fromNumber(int number) {
        final Register[] registers = values();
        if (number < 0 || number >= registers.length) {
            throw new IllegalArgumentException("register number out of range");
        }
        return registers[number];
    }

    public Register next() {
        return Register.fromNumber(this.number + 1);
    }

    public Register previous() {
        return Register.fromNumber(this.number - 1);
    }
}
