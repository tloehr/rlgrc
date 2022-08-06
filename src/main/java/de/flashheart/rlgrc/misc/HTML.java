package de.flashheart.rlgrc.misc;

import java.awt.*;

public class HTML {


    public static String document(String css, String body){
        return "<html>" + head(css)+body(body)+"</html>";
    }

    public static String head(String css) {
            return "<head>\n" + css + "</head>\n";
        }

    public static String body(String body) {
               return "<body>\n" + body + "</body>\n";
           }

    public static String ul(String content) {
        return "<ul>\n" + content + "</ul>\n";
    }

    public static String ol(String content) {
        return "<ol>\n" + content + "</ol>\n";
    }

    public static String li(String content) {
        return "<li>" + content + "</li>\n";
    }

    public static String table_th(String content, String align) {
        return "<th " + align + ">" + content + "</th>\n";
    }

    public static String table_th(String content) {
        return "<th>" + content + "</th>\n";
    }

    public static String table_th(String content, int colspan) {
        return table_th(content, null, colspan);
    }

    public static String table_th(String content, String align, int span) {
        return "<th" + (span > 1 ? " colspan=\"" + span + "\" " : "") + align + ">" + content + "</th>\n";
    }

    public static String table_td(String content, String align, int span) {
        return "<td" + (span > 1 ? " colspan=\"" + span + "\" " : "") + align + ">" + content + "</td>\n";
    }

    public static String table_td(String content, String align, String valign) {
        return "<td " + align + ">" + content + "</td>\n";
    }

    public static String table_td(String content) {
        return table_td(content, "", 0);
    }

    public static String table_td(String content, int colspan) {
        return table_td(content, null, colspan);
    }

    public static String table_td(String content, boolean bold) {
        return table_td((bold ? "<b>" : "") + content + (bold ? "</b>" : ""), null, 0);
    }


    public static String table_tr(String content) {
        return "<tr>" + content + "</tr>\n";
    }

    public static String bold(String content) {
        return "<b>" + content + "</b>";
    }

    public static String underline(String content) {
        return "<u>" + content + "</u>";
    }

    public static String italic(String content) {
        return "<i>" + content + "</i>";
    }

    public static String paragraph(String content) {
        return "<p>\n" + content + "</p>\n";
    }

    public static String div(String content) {
        return "<div>\n" + content + "</div>\n";
    }

    public static String h1(String content) {
        return "<h1 >" + content + "</h1>\n";
    }

    public static String pagebreak() {
        return "<div class=\"pagebreak\"></div>\n";
    }

    public static String h2(String content) {
        return "<h2 >" + content + "</h2>\n";
    }

    public static String h3(String content) {
        return "<h3>" + content + "</h3>\n";
    }


    public static String h4(String content) {
        return "<h4>" + content + "</h4>\n";
    }

    public static String table(String head, String body, String border) {
        return String.format("<table border=\"%s\"><thead>%s</thead><tbody>%s</tbody></table>\n",  border, head, body);
    }

    public static String color(Color color, String in) {
        return "<font color=#" + getHTMLColor(color) + ">" + in + "</font>";
    }

    public static String getHTMLColor(Color c) {
        StringBuilder sb = new StringBuilder("");

        if (c.getRed() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getRed()));

        if (c.getGreen() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getGreen()));

        if (c.getBlue() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getBlue()));

        return sb.toString();
    }

    public static String linebreak() {
        return "<br/>";
    }
}
