package com.andreidiego.mpfi.stocks.adapter.files.spreadsheets.excel.poi

enum Color private(val hex: String, val dec: Array[Byte]) {

  //  Red HTML Color Names
  case IndianRed extends Color("#CD5C5C", Array(205, 92, 92).map(_.toByte))
  case LightCoral extends Color("#F08080", Array(240, 128, 128).map(_.toByte))
  case Salmon extends Color("#FA8072", Array(250, 128, 114).map(_.toByte))
  case DarkSalmon extends Color("#E9967A", Array(233, 150, 122).map(_.toByte))
  case LightSalmon extends Color("#FFA07A", Array(255, 160, 122).map(_.toByte))
  case Crimson extends Color("#DC143C", Array(220, 20, 60).map(_.toByte))
  case Red extends Color("#FF0000", Array(255, 0, 0).map(_.toByte))
  case FireBrick extends Color("#B22222", Array(178, 34, 34).map(_.toByte))
  case DarkRed extends Color("#8B0000", Array(139, 0, 0).map(_.toByte))

  //  Pink HTML Color Names
  case Pink extends Color("#FFC0CB", Array(255, 192, 203).map(_.toByte))
  case LightPink extends Color("#FFB6C1", Array(255, 182, 193).map(_.toByte))
  case HotPink extends Color("#FF69B4", Array(255, 105, 180).map(_.toByte))
  case DeepPink extends Color("#FF1493", Array(255, 20, 147).map(_.toByte))
  case MediumVioletRed extends Color("#C71585", Array(199, 21, 133).map(_.toByte))
  case PaleVioletRed extends Color("#DB7093", Array(219, 112, 147).map(_.toByte))

  //  Orange HTML Color Names
  //  case LightSalmon extends Color("#FFA07A", Array(255, 160, 122).map(_.toByte))
  case Coral extends Color("#FF7F50", Array(255, 127, 80).map(_.toByte))
  case Tomato extends Color("#FF6347", Array(255, 99, 71).map(_.toByte))
  case OrangeRed extends Color("#FF4500", Array(255, 69, 0).map(_.toByte))
  case DarkOrange extends Color("#FF8C00", Array(255, 140, 0).map(_.toByte))
  case Orange extends Color("#FFA500", Array(255, 165, 0).map(_.toByte))

  //  Yellow HTML Color Names
  case Gold extends Color("#FFD700", Array(255, 215, 0).map(_.toByte))
  case Yellow extends Color("#FFFF00", Array(255, 255, 0).map(_.toByte))
  case LightYellow extends Color("#FFFFE0", Array(255, 255, 224).map(_.toByte))
  case LemonChiffon extends Color("#FFFACD", Array(255, 250, 205).map(_.toByte))
  case LightGoldenrodYellow extends Color("#FAFAD2", Array(250, 250, 210).map(_.toByte))
  case PapayaWhip extends Color("#FFEFD5", Array(255, 239, 213).map(_.toByte))
  case Moccasin extends Color("#FFE4B5", Array(255, 228, 181).map(_.toByte))
  case PeachPuff extends Color("#FFDAB9", Array(255, 218, 185).map(_.toByte))
  case PaleGoldenrod extends Color("#EEE8AA", Array(238, 232, 170).map(_.toByte))
  case Khaki extends Color("#F0E68C", Array(240, 230, 140).map(_.toByte))
  case DarkKhaki extends Color("#BDB76B", Array(189, 183, 107).map(_.toByte))

  //  Purple HTML Color Names
  case Lavender extends Color("#E6E6FA", Array(230, 230, 250).map(_.toByte))
  case Thistle extends Color("#D8BFD8", Array(216, 191, 216).map(_.toByte))
  case Plum extends Color("#DDA0DD", Array(221, 160, 221).map(_.toByte))
  case Violet extends Color("#EE82EE", Array(238, 130, 238).map(_.toByte))
  case Orchid extends Color("#DA70D6", Array(218, 112, 214).map(_.toByte))
  case Fuchsia extends Color("#FF00FF", Array(255, 0, 255).map(_.toByte))
  case Magenta extends Color("#FF00FF", Array(255, 0, 255).map(_.toByte))
  case MediumOrchid extends Color("#BA55D3", Array(186, 85, 211).map(_.toByte))
  case MediumPurple extends Color("#9370DB", Array(147, 112, 219).map(_.toByte))
  case RebeccaPurple extends Color("#663399", Array(102, 51, 153).map(_.toByte))
  case BlueViolet extends Color("#8A2BE2", Array(138, 43, 226).map(_.toByte))
  case DarkViolet extends Color("#9400D3", Array(148, 0, 211).map(_.toByte))
  case DarkOrchid extends Color("#9932CC", Array(153, 50, 204).map(_.toByte))
  case DarkMagenta extends Color("#8B008B", Array(139, 0, 139).map(_.toByte))
  case Purple extends Color("#800080", Array(128, 0, 128).map(_.toByte))
  case Indigo extends Color("#4B0082", Array(75, 0, 130).map(_.toByte))
  case SlateBlue extends Color("#6A5ACD", Array(106, 90, 205).map(_.toByte))
  case DarkSlateBlue extends Color("#483D8B", Array(72, 61, 139).map(_.toByte))
  //  case MediumSlateBlue extends Color("#7B68EE", Array(123, 104, 238).map(_.toByte))

  //  Green HTML Color Names
  case GreenYellow extends Color("#ADFF2F", Array(173, 255, 47).map(_.toByte))
  case Chartreuse extends Color("#7FFF00", Array(127, 255, 0).map(_.toByte))
  case LawnGreen extends Color("#7CFC00", Array(124, 252, 0).map(_.toByte))
  case Lime extends Color("#00FF00", Array(0, 255, 0).map(_.toByte))
  case LimeGreen extends Color("#32CD32", Array(50, 205, 50).map(_.toByte))
  case PaleGreen extends Color("#98FB98", Array(152, 251, 152).map(_.toByte))
  case LightGreen extends Color("#90EE90", Array(144, 238, 144).map(_.toByte))
  case MediumSpringGreen extends Color("#00FA9A", Array(0, 250, 154).map(_.toByte))
  case SpringGreen extends Color("#00FF7F", Array(0, 255, 127).map(_.toByte))
  case MediumSeaGreen extends Color("#3CB371", Array(60, 179, 113).map(_.toByte))
  case SeaGreen extends Color("#2E8B57", Array(46, 139, 87).map(_.toByte))
  case ForestGreen extends Color("#228B22", Array(34, 139, 34).map(_.toByte))
  case Green extends Color("#008000", Array(0, 128, 0).map(_.toByte))
  case DarkGreen extends Color("#006400", Array(0, 100, 0).map(_.toByte))
  case YellowGreen extends Color("#9ACD32", Array(154, 205, 50).map(_.toByte))
  case OliveDrab extends Color("#6B8E23", Array(107, 142, 35).map(_.toByte))
  case Olive extends Color("#808000", Array(128, 128, 0).map(_.toByte))
  case DarkOliveGreen extends Color("#556B2F", Array(85, 107, 47).map(_.toByte))
  case MediumAquamarine extends Color("#66CDAA", Array(102, 205, 170).map(_.toByte))
  case DarkSeaGreen extends Color("#8FBC8B", Array(143, 188, 139).map(_.toByte))
  case LightSeaGreen extends Color("#20B2AA", Array(32, 178, 170).map(_.toByte))
  case DarkCyan extends Color("#008B8B", Array(0, 139, 139).map(_.toByte))
  case Teal extends Color("#008080", Array(0, 128, 128).map(_.toByte))

  //  Blue HTML Color Names
  case Aqua extends Color("#00FFFF", Array(0, 255, 255).map(_.toByte))
  case Cyan extends Color("#00FFFF", Array(0, 255, 255).map(_.toByte))
  case LightCyan extends Color("#E0FFFF", Array(224, 255, 255).map(_.toByte))
  case PaleTurquoise extends Color("#AFEEEE", Array(175, 238, 238).map(_.toByte))
  case Aquamarine extends Color("#7FFFD4", Array(127, 255, 212).map(_.toByte))
  case Turquoise extends Color("#40E0D0", Array(64, 224, 208).map(_.toByte))
  case MediumTurquoise extends Color("#48D1CC", Array(72, 209, 204).map(_.toByte))
  case DarkTurquoise extends Color("#00CED1", Array(0, 206, 209).map(_.toByte))
  case CadetBlue extends Color("#5F9EA0", Array(95, 158, 160).map(_.toByte))
  case SteelBlue extends Color("#4682B4", Array(70, 130, 180).map(_.toByte))
  case LightSteelBlue extends Color("#B0C4DE", Array(176, 196, 222).map(_.toByte))
  case PowderBlue extends Color("#B0E0E6", Array(176, 224, 230).map(_.toByte))
  case LightBlue extends Color("#ADD8E6", Array(173, 216, 230).map(_.toByte))
  case SkyBlue extends Color("#87CEEB", Array(135, 206, 235).map(_.toByte))
  case LightSkyBlue extends Color("#87CEFA", Array(135, 206, 250).map(_.toByte))
  case DeepSkyBlue extends Color("#00BFFF", Array(0, 191, 255).map(_.toByte))
  case DodgerBlue extends Color("#1E90FF", Array(30, 144, 255).map(_.toByte))
  case CornflowerBlue extends Color("#6495ED", Array(100, 149, 237).map(_.toByte))
  case MediumSlateBlue extends Color("#7B68EE", Array(123, 104, 238).map(_.toByte))
  case RoyalBlue extends Color("#4169E1", Array(65, 105, 225).map(_.toByte))
  case Blue extends Color("#0000FF", Array(0, 0, 255).map(_.toByte))
  case MediumBlue extends Color("#0000CD", Array(0, 0, 205).map(_.toByte))
  case DarkBlue extends Color("#00008B", Array(0, 0, 139).map(_.toByte))
  case Navy extends Color("#000080", Array(0, 0, 128).map(_.toByte))
  case MidnightBlue extends Color("#191970", Array(25, 25, 112).map(_.toByte))

  //  Brown HTML Color Names
  case Cornsilk extends Color("#FFF8DC", Array(255, 248, 220).map(_.toByte))
  case BlanchedAlmond extends Color("#FFEBCD", Array(255, 235, 205).map(_.toByte))
  case Bisque extends Color("#FFE4C4", Array(255, 228, 196).map(_.toByte))
  case NavajoWhite extends Color("#FFDEAD", Array(255, 222, 173).map(_.toByte))
  case Wheat extends Color("#F5DEB3", Array(245, 222, 179).map(_.toByte))
  case BurlyWood extends Color("#DEB887", Array(222, 184, 135).map(_.toByte))
  case Tan extends Color("#D2B48C", Array(210, 180, 140).map(_.toByte))
  case RosyBrown extends Color("#BC8F8F", Array(188, 143, 143).map(_.toByte))
  case SandyBrown extends Color("#F4A460", Array(244, 164, 96).map(_.toByte))
  case Goldenrod extends Color("#DAA520", Array(218, 165, 32).map(_.toByte))
  case DarkGoldenrod extends Color("#B8860B", Array(184, 134, 11).map(_.toByte))
  case Peru extends Color("#CD853F", Array(205, 133, 63).map(_.toByte))
  case Chocolate extends Color("#D2691E", Array(210, 105, 30).map(_.toByte))
  case SaddleBrown extends Color("#8B4513", Array(139, 69, 19).map(_.toByte))
  case Sienna extends Color("#A0522D", Array(160, 82, 45).map(_.toByte))
  case Brown extends Color("#A52A2A", Array(165, 42, 42).map(_.toByte))
  case Maroon extends Color("#800000", Array(128, 0, 0).map(_.toByte))

  //  White HTML Color Names
  case White extends Color("#FFFFFF", Array(255, 255, 255).map(_.toByte))
  case Snow extends Color("#FFFAFA", Array(255, 250, 250).map(_.toByte))
  case HoneyDew extends Color("#F0FFF0", Array(240, 255, 240).map(_.toByte))
  case MintCream extends Color("#F5FFFA", Array(245, 255, 250).map(_.toByte))
  case Azure extends Color("#F0FFFF", Array(240, 255, 255).map(_.toByte))
  case AliceBlue extends Color("#F0F8FF", Array(240, 248, 255).map(_.toByte))
  case GhostWhite extends Color("#F8F8FF", Array(248, 248, 255).map(_.toByte))
  case WhiteSmoke extends Color("#F5F5F5", Array(245, 245, 245).map(_.toByte))
  case SeaShell extends Color("#FFF5EE", Array(255, 245, 238).map(_.toByte))
  case Beige extends Color("#F5F5DC", Array(245, 245, 220).map(_.toByte))
  case OldLace extends Color("#FDF5E6", Array(253, 245, 230).map(_.toByte))
  case FloralWhite extends Color("#FFFAF0", Array(255, 250, 240).map(_.toByte))
  case Ivory extends Color("#FFFFF0", Array(255, 255, 240).map(_.toByte))
  case AntiqueWhite extends Color("#FAEBD7", Array(250, 235, 215).map(_.toByte))
  case Linen extends Color("#FAF0E6", Array(250, 240, 230).map(_.toByte))
  case LavenderBlush extends Color("#FFF0F5", Array(255, 240, 245).map(_.toByte))
  case MistyRose extends Color("#FFE4E1", Array(255, 228, 225).map(_.toByte))

  //  Gray HTML Color Names
  case Gainsboro extends Color("#DCDCDC", Array(220, 220, 220).map(_.toByte))
  case LightGray extends Color("#D3D3D3", Array(211, 211, 211).map(_.toByte))
  case Silver extends Color("#C0C0C0", Array(192, 192, 192).map(_.toByte))
  case DarkGray extends Color("#A9A9A9", Array(169, 169, 169).map(_.toByte))
  case Gray extends Color("#808080", Array(128, 128, 128).map(_.toByte))
  case DimGray extends Color("#696969", Array(105, 105, 105).map(_.toByte))
  case LightSlateGray extends Color("#778899", Array(119, 136, 153).map(_.toByte))
  case SlateGray extends Color("#708090", Array(112, 128, 144).map(_.toByte))
  case DarkSlateGray extends Color("#2F4F4F", Array(47, 79, 79).map(_.toByte))
  case Black extends Color("#000000", Array(0, 0, 0).map(_.toByte))
}

object Color:
  def apply(hex: String): Option[Color] = values.find(color ⇒ color.hex == hex)

  def apply(red: Byte, green: Byte, blue: Byte): Option[Color] = values.find(color ⇒ color.dec.sameElements(Array(red, green, blue)))