package otld.otld.parsing;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Syntax Error listener for the otld parser.
 */
public class otldErrorListener extends BaseErrorListener {
    /** List of encountered syntax errors. */
    private List<String> errors;

    public otldErrorListener() {
        this.errors = new ArrayList<>();
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {

        errors.add(
                new Error(line,
                          charPositionInLine,
                          ErrorMsg.SYNTAXERROR.getMessage(),
                          offendingSymbol.toString().charAt(0)
                ).getError()
        );
    }

    /** Returns the list of syntax errors that were encountered during parsing. */
    public List<String> getErrors() {
        return this.errors;
    }
}