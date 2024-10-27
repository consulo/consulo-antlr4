package consulo.antlr.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.antlr.intellij.plugin.templates.OutsideRuleContext;

@ExtensionImpl
public class ANTLR4LiveTemplateContributor implements LiveTemplateContributor {
  @Override
  @Nonnull
  public String groupId() {
    return "antlr4";
  }

  @Override
  @Nonnull
  public LocalizeValue groupName() {
    return LocalizeValue.localizeTODO("ANTLR4");
  }

  @Override
  public void contribute(@Nonnull Factory factory) {
    try(Builder builder = factory.newBuilder("antlr4Rid", "rid", "$NAME$ : [a-zA-Z_]+ [a-zA-Z0-9_]* ;", LocalizeValue.localizeTODO("Create identifier rule"))) {
      builder.withVariable("NAME", "", "\"ID\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rflt", "rflt", "$NAME$\n"
        + "    :   '-'? INT '.' INT EXP?   // 1.35, 1.35E-9, 0.3, -4.5\n"
        + "    |   '-'? INT EXP            // 1e10 -3e4\n"
        + "    |   '-'? INT                // -3, 45\n"
        + "    ;\n"
        + "\n"
        + "fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros\n"
        + "fragment EXP :   [Ee] [+\\-]? INT ;", LocalizeValue.localizeTODO("Create simple float rule"))) {
      builder.withVariable("NAME", "", "\"FLOAT\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rint", "rint", "$NAME$ : [0-9]+ ;", LocalizeValue.localizeTODO("Create integer rule"))) {
      builder.withVariable("NAME", "", "\"INT\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rws", "rws", "$NAME$ : [ \\t\\n\\r]+ -> channel(HIDDEN) ;", LocalizeValue.localizeTODO("Create whitespace rule"))) {
      builder.withVariable("NAME", "", "\"WS\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rcmt", "rcmt", "$NAME$ : '/*' .*? '*/' -> channel(HIDDEN) ;", LocalizeValue.localizeTODO("Create C comment rule"))) {
      builder.withVariable("NAME", "", "\"COMMENT\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rlcmt", "rlcmt", "$NAME$ : '//' ~'\\n'* '\\n' -> channel(HIDDEN) ;", LocalizeValue.localizeTODO("Create C line comment rule"))) {
      builder.withVariable("NAME", "", "\"LINE_COMMENT\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rstr", "rstr", "$NAME$ : '\"' (~'\"'|'\\\\\"')* '\"'  ;", LocalizeValue.localizeTODO("Create simple string rule"))) {
      builder.withVariable("NAME", "", "\"STRING\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rstr", "rstr", "$NAME$ : '\"' (~'\"'|'\\\\\"')* '\"'  ;", LocalizeValue.localizeTODO("Create simple string rule"))) {
      builder.withVariable("NAME", "", "\"STRING\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

    try(Builder builder = factory.newBuilder("antlr4Rstr2", "rstr2", "$NAME$ :  '\"' (ESC | ~[\"\\\\])* '\"' ;\n"
        + "\n"
        + "fragment ESC :   '\\\\' ([\"\\\\/bfnrt] | UNICODE) ;\n"
        + "fragment UNICODE : 'u' HEX HEX HEX HEX ;\n"
        + "fragment HEX : [0-9a-fA-F] ;\n", LocalizeValue.localizeTODO("Create string w/escapes rule"))) {
      builder.withVariable("NAME", "", "\"STRING\"", true);

      builder.withContext(OutsideRuleContext.class, true);
    }

  }
}
