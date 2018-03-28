import argparse
import re

def analyze_file(filename):
    total_frags_correct = 0
    total_frags = 0
    total_queries = 0

    with open(filename) as f:
        phrases = {}
        for line in f:
            if "::" in line:
                phrase, result = line.split("::")
                answers_str, guess_str = result.split(':')

                if phrase not in phrases:
                    phrases[phrase] = True

                answers = []
                for ans in answers_str.split(";"):
                    answers.append(ans.strip())

                m = re.match('(.*)\([0-9.]+\)', guess_str)
                guess = m.group(1).strip()
                phrases[phrase] = phrases[phrase] and (guess in answers)
            # end of query
            if line.startswith('== RESULT'):
                for phrase, result in phrases.iteritems():
                    if result:
                        total_frags_correct += 1
                    total_frags += 1
                total_queries += 1
                phrases = {}

    print '==== FINAL STATS ===='
    print 'Correct frags: ' + str(total_frags_correct)
    print 'Total frags: ' + str(total_frags)
    print 'Total queries: ' + str(total_queries)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('filename')
    args = parser.parse_args()

    analyze_file(args.filename)


if __name__ == '__main__':
    main()
