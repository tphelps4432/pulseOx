package com.example.tomphelps.pulseox;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Tom Phelps on 11/11/2016.
 */
public class ParallelWriter implements Runnable{
    private BlockingQueue<Item> q;
    private int indentation;
    private PrintWriter out;
    public ParallelWriter(File f, PrintWriter out ){
        q = new LinkedBlockingQueue<Item>();
        indentation = 0;
        this.out=out;
        out.println(" Testing2");
    }

    public ParallelWriter append( CharSequence str ){
        try {
            CharSeqItem item = new CharSeqItem();
            item.content = str;
            item.type = ItemType.CHARSEQ;
            q.put(item);
            return this;
        } catch (InterruptedException ex) {
            throw new RuntimeException( ex );
        }
    }

    public ParallelWriter newLine(){
        try {
            Item item = new Item();
            item.type = ItemType.NEWLINE;
            q.put(item);
            return this;
        } catch (InterruptedException ex) {
            throw new RuntimeException( ex );
        }
    }

    public void setIndent(int indentation) {
        try{
            IndentCommand item = new IndentCommand();
            item.type = ItemType.INDENT;
            item.indent = indentation;
            q.put(item);
        } catch (InterruptedException ex) {
            throw new RuntimeException( ex );
        }
    }

    public void end(){
        try {
            Item item = new Item();
            item.type = ItemType.POISON;
            q.put(item);
        } catch (InterruptedException ex) {
            throw new RuntimeException( ex );
        }
    }

    public void run() {
        Item item = null;

        try{
//                out = new PrintWriter(new BufferedWriter( new FileWriter( mfile ) ));
            while( (item = q.take()).type != ItemType.POISON ){
                switch( item.type ){
                    case NEWLINE:
                        out.println("");
                        for( int i = 0; i < indentation; i++ )
                            out.append("   ");
                        break;
                    case INDENT:
                        indentation = ((IndentCommand)item).indent;
                        break;
                    case CHARSEQ:
                        out.println( ((CharSeqItem)item).content );
                }
            }
        } catch (InterruptedException ex){
            throw new RuntimeException( ex );
        } catch  (Exception e) {
            throw new RuntimeException( e );
        } finally {
            if( out != null ) try {
                out.flush();
                out.close();

//                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                    intent.setData(Uri.fromFile(dataFile));
//                    context.sendBroadcast(intent);
            } catch (Exception ex) {
                throw new RuntimeException( ex );
            }
        }
    }

    private enum ItemType {
        CHARSEQ, NEWLINE, INDENT, POISON;
    }
    private static class Item {
        ItemType type;
    }
    private static class CharSeqItem extends Item {
        CharSequence content;
    }
    private static class IndentCommand extends Item {
        int indent;
    }
}
