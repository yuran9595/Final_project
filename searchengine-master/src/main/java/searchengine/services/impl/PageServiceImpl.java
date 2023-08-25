package searchengine.services.impl;

import org.springframework.stereotype.Service;
import searchengine.services.PageService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class PageServiceImpl implements PageService {
    @Override
    public String getTitle(String content) {
        String regex = "(?<=<title>).*?(?=<\\/title>)";
        content = content.replaceAll("\\s+", " ");
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(content);
        if (matcher.find()) {
            return  matcher.group();
        }
        return " ";
    }
}
