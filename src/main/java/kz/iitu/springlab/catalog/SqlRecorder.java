package kz.iitu.springlab.catalog;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import java.util.*;
public class SqlRecorder implements StatementInspector {
    private static final ThreadLocal<List<String>> CURRENT=new ThreadLocal<>();
    public static void begin(){CURRENT.set(new ArrayList<>());}
    public static List<String> end(){List<String> sql=CURRENT.get();CURRENT.remove();return sql==null?List.of():List.copyOf(sql);}
    @Override public String inspect(String sql){List<String> list=CURRENT.get();if(list!=null)list.add(sql);return sql;}
}
