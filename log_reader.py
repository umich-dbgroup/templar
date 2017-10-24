import re
import sys

c1_count = 14
c2_count = 59
c3_count = 60
c4_count = 63

c1_top1 = 0
c1_top3 = 0
c1_top5 = 0
c2_top1 = 0
c2_top3 = 0
c2_top5 = 0
c3_top1 = 0
c3_top3 = 0
c3_top5 = 0
c4_top1 = 0
c4_top3 = 0
c4_top5 = 0
p = re.compile(".*QUERY ([0-9]+): ([0-9]+)")
with open(sys.argv[1]) as f:
    for line in f:
        m = p.match(line)
        if m is not None:
            query_id = int(m.group(1))
            rank = int(m.group(2))

            if query_id < c1_count:
                if rank <= 5:
                    c1_top5 += 1
                if rank <= 3:
                    c1_top3 += 1
                if rank == 1:
                    c1_top1 += 1
            elif query_id < c1_count + c2_count:
                if rank <= 5:
                    c2_top5 += 1
                if rank <= 3:
                    c2_top3 += 1
                if rank == 1:
                    c2_top1 += 1
            elif query_id < c1_count + c2_count + c3_count:
                if rank <= 5:
                    c3_top5 += 1
                if rank <= 3:
                    c3_top3 += 1
                if rank == 1:
                    c3_top1 += 1
            else:
                if rank <= 5:
                    c4_top5 += 1
                if rank <= 3:
                    c4_top3 += 1
                if rank == 1:
                    c4_top1 += 1

print("c1 TOP 1: " + str(c1_top1))
print("c1 TOP 3: " + str(c1_top3))
print("c1 TOP 5: " + str(c1_top5))
print("c2 TOP 1: " + str(c2_top1))
print("c2 TOP 3: " + str(c2_top3))
print("c2 TOP 5: " + str(c2_top5))
print("c3 TOP 1: " + str(c3_top1))
print("c3 TOP 3: " + str(c3_top3))
print("c3 TOP 5: " + str(c3_top5))
print("c4 TOP 1: " + str(c4_top1))
print("c4 TOP 3: " + str(c4_top3))
print("c4 TOP 5: " + str(c4_top5))
