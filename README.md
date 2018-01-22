# TwitterOpinionMining


Use commands for each part of the project.  
Command `collect`: Parts 1 & 2, Collect and store tweets.  
Command `print`: Collection with tweets printing.  
Command `tweet-analyze`: Parts 3 & 4, Transform and sentiment analysis in tweets.  
Command `user-analyze`: Part 5, User analysis.  

Help message:  
```
java -cp out/artifacts/core_jar/core.jar Main --help
```

    Usage: Main [options] [command] [command options]
      Options:
        -h, --help
          Prints help message
      Commands:
        collect      Collects tweets and stores them in a mongoDB collection
          Usage: collect [options] <search keyword>
            Options:
              -h, --help
                Prints help message
              -H, --mongoHost
                MongoDB Host
                Default: localhost
              -p, --mongoPort
                MongoDB Port
                Default: 27017
    
        print-collection      Prints entries of a mongoDB collection
          Usage: print-collection [options] <search keyword>
            Options:
              -h, --help
                Prints help message
              -H, --mongoHost
                MongoDB Host
                Default: localhost
              -p, --mongoPort
                MongoDB Port
                Default: 27017
              -s, --short
                Short printing
                Default: false
    
        tweet-analyze      Makes sentiment analysis in tweets
          Usage: tweet-analyze [options] <search keyword>
            Options:
              -d, --charts-directory
                Directory that charts will be stored.
                Default: .
              -h, --help
                Prints help message
              -H, --mongoHost
                MongoDB Host
                Default: localhost
              -p, --mongoPort
                MongoDB Port
                Default: 27017
    
        user-analyze      Makes sentiment analysis in tweets
          Usage: user-analyze [options] <search keyword>
            Options:
              -d, --charts-directory
                Directory that charts will be stored.
                Default: .
              -h, --help
                Prints help message
              -H, --mongoHost
                MongoDB Host
                Default: localhost
              -p, --mongoPort
                MongoDB Port
                Default: 27017


