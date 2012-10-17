package org.teatrove.teaadmin.viewer;

import java.util.ArrayList;
import java.util.List;

import org.teatrove.tea.compiler.SourceInfo;
import org.teatrove.tea.parsetree.AndExpression;
import org.teatrove.tea.parsetree.Block;
import org.teatrove.tea.parsetree.BooleanLiteral;
import org.teatrove.tea.parsetree.BreakStatement;
import org.teatrove.tea.parsetree.ContinueStatement;
import org.teatrove.tea.parsetree.Expression;
import org.teatrove.tea.parsetree.ForeachStatement;
import org.teatrove.tea.parsetree.FunctionCallExpression;
import org.teatrove.tea.parsetree.IfStatement;
import org.teatrove.tea.parsetree.ImportDirective;
import org.teatrove.tea.parsetree.NotExpression;
import org.teatrove.tea.parsetree.NullLiteral;
import org.teatrove.tea.parsetree.NumberLiteral;
import org.teatrove.tea.parsetree.OrExpression;
import org.teatrove.tea.parsetree.StringLiteral;
import org.teatrove.tea.parsetree.Template;
import org.teatrove.tea.parsetree.TemplateCallExpression;
import org.teatrove.tea.parsetree.TreeWalker;
import org.teatrove.tea.parsetree.TypeName;
import org.teatrove.teaadmin.viewer.TemplateView.Callee;
import org.teatrove.teaadmin.viewer.TemplateViewerApplication.State;

public class SourceWalker extends TreeWalker {

    private RewriteUnit unit;
    private StringBuilder buffer;
    private List<CalleeInfo> callees = new ArrayList<CalleeInfo>();
    
    protected static final char LEFT_BRACKET = (char) 1;
    protected static final char RIGHT_BRACKET = (char) 2;
    
    public SourceWalker(StringBuilder buffer) {
        this.buffer = buffer;
        this.unit = new RewriteUnit(buffer);
    }

    public void finish(TemplateView view) {

        // add list of callees
        // NOTE: this must occur before the buffer is modified below in order to
        // preserve the rewrite offsets
        for (CalleeInfo callee : callees) {
            Callee result = new Callee
            (
                callee.info.getLine(), callee.name, 
                unescape(unit.read(callee.info)), callee.isTemplate
            );
            
            view.addCallee(result);
        }
        
        // clean the source and then unescape previously escaped codes
        cleanSource();
        
        // set the resulting buffer as the source code
        view.setSourceCode(unescape(buffer.toString()));
    }
    
    @Override
    public Object visit(AndExpression node) {
        rewrite(unit, node.getOperator().getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(BooleanLiteral node) {
        rewrite(unit, node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(BreakStatement node) {
        rewrite(unit, node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(ContinueStatement node) {
        rewrite(unit, node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(ForeachStatement node) {
        SourceInfo info = node.getSourceInfo();
        info = info.setEndPosition(info.getStartPosition() + 6);
        rewrite(unit, info);
        return super.visit(node);
    }

    @Override
    public Object visit(IfStatement node) {
        SourceInfo info = node.getSourceInfo();
        info = info.setEndPosition(info.getStartPosition() + 1);
        rewrite(unit, info);
        
        Block block = node.getElsePart();
        if (block != null) {
            SourceInfo bsrc = block.getSourceInfo();
            Block then = node.getThenPart();
            SourceInfo tsrc = then.getSourceInfo();
            SourceInfo src = new SourceInfo
            (
                bsrc.getLine(), tsrc.getEndPosition() + 1,
                bsrc.getStartPosition() - 1
            );
            
            rewrite(unit, src);
        }
        
        return super.visit(node);
    }

    @Override
    public Object visit(ImportDirective node) {
        SourceInfo info = node.getSourceInfo();
        info = info.setEndPosition(info.getStartPosition() + 5);
        rewrite(unit, info);
        return super.visit(node);
    }

    @Override
    public Object visit(NotExpression node) {
        SourceInfo info = node.getSourceInfo();
        info = info.setEndPosition(info.getStartPosition() + 2);
        rewrite(unit, info);
        return super.visit(node);
    }

    @Override
    public Object visit(OrExpression node) {
        rewrite(unit, node.getOperator().getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(Template node) {
        SourceInfo info = node.getSourceInfo();
        info = info.setEndPosition(info.getStartPosition() + 7);
        rewrite(unit, info);
        return super.visit(node);
    }

    @Override
    public Object visit(TypeName node) {
        rewrite(unit, "type", node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(NullLiteral node) {
        rewrite(unit, node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(NumberLiteral node) {
        rewrite(unit, "number", node.getSourceInfo());
        return super.visit(node);
    }

    @Override
    public Object visit(StringLiteral node) {
        SourceInfo info = node.getSourceInfo();
        String value = unit.read(info);
        if (!value.contains("\n")) {
            rewrite(unit, "string", info);
        }

        return super.visit(node);
    }

    @Override
    public Object visit(TemplateCallExpression node) {
        SourceInfo info = node.getSourceInfo();
        callees.add(new CalleeInfo(node.getTarget().getName(), info, true));
        
        String value = unit.read(info);
        int sidx = value.indexOf(' ');
        int pidx = value.indexOf('(', sidx);
        
        StringBuilder result = new StringBuilder(pidx + 48);
        result.append(escape("<span class=\"keyword\">"))
              .append(value.substring(0, sidx + 1))
              .append(escape("<span class=\"call\">"))
              .append(escape("<a href=\"#\">"))
              .append(value.substring(sidx + 1, pidx))
              .append(escape("</a></span>"));

        SourceInfo src =
            info.setEndPosition(info.getStartPosition() + pidx - 1);
        unit.rewrite(new Rewrite(result.toString(), src));
        
        return super.visit(node);
    }

    @Override
    public Object visit(FunctionCallExpression node) {
        SourceInfo info = node.getTarget().getSourceInfo();
        info = info.setEndPosition(node.getSourceInfo().getEndPosition());
        
        Expression expr = node.getExpression();
        if (expr == null) {
            callees.add(new CalleeInfo(node.getTarget().getName(), info, false));
        }
        
        String value = unit.read(info);
        int idx = value.indexOf('(');
        
        StringBuilder result = new StringBuilder(idx + 48);
        result.append(escape("<span class=\"function\">"));
        if (expr == null) {
            result.append(escape("<a href=\"#\">"))
                  .append(value.substring(0, idx))
                  .append(escape("</a>"));
        }
        else {
            result.append(value.substring(0, idx));
        }

        result.append(escape("</span>"));

        SourceInfo src =
            info.setEndPosition(info.getStartPosition() + idx - 1);
        unit.rewrite(new Rewrite(result.toString(), src));

        return super.visit(node);
    }

    protected String nextLine(int line, String span) {
        StringBuilder result = new StringBuilder(128);
        if (line > 1) {
            result.append("</span></li>\n");
        }
        
        result.append("<li><a name=\"line").append(line)
              .append("\"></a><span class=\"")
              .append(span).append("\">");
        
        return escape(result.toString());
    }
    
    protected String escape(String value) {
        return value.replace('<', LEFT_BRACKET).replace('>', RIGHT_BRACKET);
    }
    
    protected String unescape(String value) {
        return value.replace(LEFT_BRACKET, '<').replace(RIGHT_BRACKET, '>');
    }

    protected void rewrite(RewriteUnit unit, SourceInfo info) {
        rewrite(unit, "keyword", info);
    }
    
    protected void rewrite(RewriteUnit unit, String type, SourceInfo info) {
        String value = unit.read(info);
        StringBuilder result = new StringBuilder(value.length() + 48);
        result.append(escape("<span class=\""))
              .append(type).append(escape("\">")).append(value)
              .append(escape("</span>"));
        
        unit.rewrite(new Rewrite(result.toString(), info));
    }

    
    protected void cleanSource() {
        char ch = 0;
        int line = 1;
        String span = "text";
        boolean inTag = false;
        State state = State.DEFAULT;

        // inject first line
        String value = nextLine(line++, span);
        buffer.insert(0, value);

        // walk the buffer fixing HTML tags and injecting code/template tags
        for (int i = 0; i < buffer.length(); i++) {
            ch = buffer.charAt(i);
            
            // handle new lines and inject end of line/start of line tags
            if (ch == '\r') { continue; }
            else if (ch == '\n') {
                if (state == State.SINGLE_COMMENT) {
                    span = "template";
                    state = State.TEMPLATE;

                    String insert = escape("</span><span class=\"template\">");
                    buffer.insert(i, insert);

                    i += insert.length();
                }

                value = nextLine(line++, span); 
                buffer.replace(i, i + 1, value);
                i += value.length() - 1;
                continue;
            }

            // check if we are in a previous escaped tag and ignore chars
            // between (previously generated HTML that shouldn't escape)
            if (inTag) {
                if (ch == RIGHT_BRACKET) { 
                    inTag = false; 
                }
                continue; 
            }
            else if (ch == LEFT_BRACKET) {
                inTag = true;
                continue;
            }

            // handle state when we are in a template and check if end of state
            if (state == State.TEMPLATE) {
                if (ch == '%' && i + 1 < buffer.length() &&
                    buffer.charAt(i + 1) == '>') {
                    
                    span = "text";
                    state = State.DEFAULT;
                    
                    String insert = 
                        escape("%&gt;</span><span class=\"text\">");
                    buffer.replace(i, i + 2,  insert);
                    i += insert.length() - 1; 
                    
                    continue;
                }
                else if (ch == '\'') {
                    state = State.SINGLE_QUOTE;
                    continue;
                }
                else if (ch == '"') {
                    state = State.DOUBLE_QUOTE;
                    continue;
                }
                else if (ch == '/' && i + 1 < buffer.length()) {
                    char next = buffer.charAt(i + 1);
                    if (next == '/' || next == '*') {
                        span = "comment";
                        state = (next == '/' ? State.SINGLE_COMMENT 
                            : State.MULTI_COMMENT);
                        
                        String insert = 
                            escape("</span><span class=\"comment\">");
                        buffer.insert(i, insert);
                        i += insert.length() + 1;
                        
                        continue;
                    }
                }
            }

            // handle quoted strings to ignore other actions within a quote
            else if (state == State.SINGLE_QUOTE || 
                     state == State.DOUBLE_QUOTE) {
                
                char quote = (state == State.SINGLE_QUOTE ? '\'' : '"');
                if (ch == quote) {
                    // attempt to walk up escape chars to determine if quote is
                    // escaped or not
                    int count = 0;
                    for (int j = i - 1; j >= 0; j--) {
                        if (buffer.charAt(j) != '\\') { break; }
                        else { count++; }
                    }
                    
                    // even amounts of previous escape chars means quote not
                    // escaped
                    if (count % 2 == 0) {
                        state = State.TEMPLATE;
                        continue;
                    }
                }
            }
            
            // handle state when we are in a comment
            else if (state == State.MULTI_COMMENT) {
                if (ch == '*' && i + 1 < buffer.length() &&
                    buffer.charAt(i + 1) == '/') {
                    
                    span = "template";
                    state = State.TEMPLATE;
                    
                    String insert = escape("</span><span class=\"template\">");
                    buffer.insert(i + 2, insert);
                    i += insert.length() + 1;
                    
                    continue;
                }
            }
            
            // handle state when we are in a text and check if start of
            // code
            else if (state == State.DEFAULT) {
                if (ch == '<' && i + 1 < buffer.length() &&
                    buffer.charAt(i + 1) == '%') {
                    
                    span = "template";
                    state = State.TEMPLATE;
                    
                    String insert =
                        escape("</span><span class=\"template\">&lt;%");
                    buffer.replace(i, i + 2, insert);
                    i += insert.length() - 1;
                    
                    continue;
                }
            }
            
            // otherwise, escape to proper HTML
            String replace = null;
            if (ch == '<') { replace = "&lt;"; }
            else if (ch == '>') { replace = "&gt;"; }
            else if (ch == '&') { replace = "&amp;"; }
            else if (ch == ' ') { replace = "&nbsp;"; }
            
            if (replace != null) {
                buffer.replace(i, i + 1, replace);
                i += replace.length() - 1;
            }
        }
        
        // add end of line
        buffer.append(escape("</span></li>"));
    }

    protected static class CalleeInfo {
        String name;
        SourceInfo info;
        boolean isTemplate;
        
        public CalleeInfo(String name, SourceInfo info, boolean isTemplate) {
            this.name = name;
            this.info = info;
            this.isTemplate = isTemplate;
        }
    }
}
